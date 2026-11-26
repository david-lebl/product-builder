package mpbuilder.admin.views

import kyo.*
import kyo.test.*
import mpbuilder.admin.i18n.AdminTranslations
import mpbuilder.admin.port.BackofficeClient
import mpbuilder.admin.state.{AdminRoute, BackofficeState}
import mpbuilder.admin.views.customers.*
import mpbuilder.admin.views.customers.CustomerDrafts.*
import mpbuilder.customers.*
import mpbuilder.customers.sample.SampleCustomers
import mpbuilder.sample.SampleShowcaseRuntime
import mpbuilder.shared.*
import mpbuilder.ui.components.ConfirmDialog
import mpbuilder.ui.i18n.AppI18n

class CustomerManagerSpec extends Test[Any]:

  private def withState[A](f: (BackofficeState, AppI18n) => A < (Abort[Any] & Async)): A < (Abort[Any] & Async) =
    for
      rt    <- SampleShowcaseRuntime.make()
      i18n  <- AdminTranslations.init(Language.En)
      state <- BackofficeState.init(BackofficeClient.fromFacade(rt.backoffice), i18n)
      _     <- I18n.let(i18n.handle)(state.login("admin@printshop.cz", "staff1234"))
      _     <- I18n.let(i18n.handle)(state.navigate(AdminRoute.Customers))
      res   <- f(state, i18n)
    yield res

  private def renderHtml(ui: UI, i18n: AppI18n): String < Async =
    I18n.let(i18n.handle)(UI.runRender(ui).take(1).run.map(_.headMaybe.getOrElse("")))

  private def noMissingKeys(html: String): Boolean = !html.contains("‹") && !html.contains("›")

  "Customer drafts" - {
    "suggest readable, collision-free IDs (diacritics stripped)" in {
      val d = RegisterDraft(companyName = "Ateliér Nová s.r.o.")
      assert(suggestedId(d, Set.empty) == "cust-atelier-nova-s-r-o")
      assert(suggestedId(d, Set("cust-atelier-nova-s-r-o")) == "cust-atelier-nova-s-r-o-2")
      val person = RegisterDraft(segment = CustomerSegment.Individual, contact = ContactDraft("Jana", "Černá", "", "", ""))
      assert(suggestedId(person, Set.empty) == "cust-jana-cerna")
    }

    "registration validation flags required fields, company for B2B, taken emails and IDs" in {
      val empty = validateRegister(RegisterDraft(), Set.empty, Map.empty, isCs = false)
      assert(Set("companyName", "regNo", "contact.firstName", "contact.lastName", "contact.email", "billing.street").subsetOf(empty.keySet))
      val individual = validateRegister(RegisterDraft(segment = CustomerSegment.Individual), Set.empty, Map.empty, isCs = false)
      assert(!individual.contains("companyName"))

      val filled = RegisterDraft(
        companyName = "Nova",
        regNo = "123",
        contact = ContactDraft("Tereza", "Králová", "Tereza@Nova.cz", "", ""),
        billing = AddressDraft("Main 1", "Praha", "110 00", "CZ"),
      )
      assert(validateRegister(filled, Set.empty, Map.empty, isCs = false).isEmpty)
      val clash = validateRegister(filled.copy(customId = "cust-x"), Set("cust-x"), Map("tereza@nova.cz" -> "Other Co"), isCs = false)
      assert(clash.get("customId").exists(_.contains("already in use")))
      assert(clash.get("contact.email").exists(_.contains("Other Co")))
      assert(validateRegister(filled.copy(customId = "Bad ID"), Set.empty, Map.empty, isCs = false).contains("customId"))

      val cmd = registerCmd(filled, Set.empty)
      assert(cmd.id.value == "cust-nova")
      assert(cmd.contact.email.value == "tereza@nova.cz")
      assert(cmd.company.exists(_.contactPerson.value == "Tereza Králová"))
    }

    "profile quick actions promote contacts and keep the default delivery address consistent" in {
      withState { (state, _) =>
        for
          custs <- state.customers.get
          c     <- Abort.get(custs.find(_.id == SampleCustomers.printShopProId).toRight("sample customer missing"))
        yield
          val promoted = promoteContactCmd(c, 0).get
          assert(promoted.contact == c.additionalContacts.head)
          assert(promoted.additionalContacts.exists(_.head == c.contact))
          assert(promoted.company.exists(_.contactPerson.value == CustomerUi.personName(c.additionalContacts.head)))

          val removed = removeShippingCmd(c.copy(defaultShippingIndex = Some(1)), 0)
          assert(removed.defaultShippingIndex.contains(0))

          val draft = ProfileDraft.from(c, Section.Contacts)
          assert(validateProfile(draft, c.segment, Map.empty, isCs = false).isEmpty)
          val dupe = draft.copy(extras = draft.extras :+ draft.primary)
          assert(validateProfile(dupe, c.segment, Map.empty, isCs = false).keySet.exists(_.endsWith(".email")))
          assert(profileCmd(draft, c) == currentCmd(c).copy(company = profileCmd(draft, c).company))
      }
    }

    "payment terms draft round-trips current terms and validates the 1–90 day window" in {
      withState { (state, _) =>
        for
          custs <- state.customers.get
          c     <- Abort.get(custs.find(_.id == SampleCustomers.printShopProId).toRight("sample customer missing"))
        yield
          val d = TermsDraft.from(c)
          assert(d.kind == TermsKind.Net)
          assert(termsValue(d) == c.paymentTerms)
          assert(d.sameAs(d.copy(limit = s"${d.limit}.00")))
          assert(validateTerms(d.copy(dueDays = "120"), isCs = false).contains("dueDays"))
          assert(validateTerms(d.copy(limit = "0"), isCs = false).contains("limit"))
      }
    }
  }

  "Customer Manager screen" - {
    "registers a customer, returns its id, and rejects a duplicate id" in {
      withState { (state, i18n) =>
        val draft = RegisterDraft(
          companyName = "Atelier Nova s.r.o.",
          regNo = "28991122",
          contact = ContactDraft("Tereza", "Králová", "tereza@ateliernova.cz", "", ""),
          billing = AddressDraft("Vinohradská 12", "Praha", "120 00", "CZ"),
        )
        for
          before <- state.customers.get
          ids     = before.map(_.id.value).toSet
          created <- I18n.let(i18n.handle)(state.registerCustomer(registerCmd(draft, ids)))
          after   <- state.customers.get
          dup     <- I18n.let(i18n.handle)(
                       state.registerCustomer(registerCmd(draft.copy(contact = draft.contact.copy(email = "other@ateliernova.cz"), customId = created.get.value), ids))
                     )
          banner  <- state.errorBanner.get
        yield
          assert(created.exists(_.value == "cust-atelier-nova-s-r-o"))
          assert(after.exists(c => created.contains(c.id) && c.status == CustomerStatus.PendingApproval))
          assert(dup.isEmpty)
          assert(banner.exists(_.contains("already in use")))
      }
    }

    "opens the selected customer with KPI tiles, status-aware actions and per-section edit buttons (EN & CS)" in {
      withState { (state, i18n) =>
        for
          _      <- I18n.let(i18n.handle)(state.selectCustomer(SampleCustomers.printShopProId))
          ui     <- I18n.let(i18n.handle)(CustomerManagerView.render(state))
          en     <- renderHtml(ui, i18n)
          _      <- I18n.let(i18n.handle)(state.setLanguage(Language.Cs))
          cs     <- renderHtml(ui, i18n)
          _      <- I18n.let(i18n.handle)(state.setLanguage(Language.En))
          _      <- I18n.let(i18n.handle)(state.selectCustomer(SampleCustomers.pendingCoId))
          pendUi <- I18n.let(i18n.handle)(CustomerManagerView.render(state))
          pend   <- renderHtml(pendUi, i18n)
        yield
          assert(en.contains("mp-cust-stats"))
          assert(en.contains("Lifetime value"))
          assert(en.contains("Pricing &amp; Terms"))
          assert(en.contains("Make primary"))
          assert(en.contains("Set as default") || en.contains("Default"))
          assert(en.contains(">Edit<"))
          assert(en.contains("Add note"))
          assert(cs.contains("Celková hodnota"))
          assert(cs.contains("Nastavit jako hlavní"))
          assert(noMissingKeys(en) && noMissingKeys(cs))
          assert(pend.contains("Awaiting approval"))
          assert(pend.contains("Approve Account"))
          assert(noMissingKeys(pend))
      }
    }

    "Pricing & Terms tab shows credit usage, tier preview and agreements; editing profile saves through the state (EN & CS)" in {
      withState { (state, i18n) =>
        for
          custs   <- state.customers.get
          agrs    <- state.agreements.get
          orders  <- state.orders.get
          c       <- Abort.get(custs.find(_.id == SampleCustomers.printShopProId).toRight("sample customer missing"))
          stats    = CustomerUi.orderStats(orders.filter(_.customer.customerId.contains(c.id)))
          tierRef <- Signal.initRef[Option[(CustomerId, CustomerTier)]](CustomerTier.values.find(_ != c.tier).map(c.id -> _))
          termRef <- Signal.initRef[Option[TermsDraft]](Some(TermsDraft.from(c).copy(dueDays = "30")))
          refs     = CustomerCommercialTab.Refs(tierRef, termRef)
          en      <- renderHtml(CustomerCommercialTab.render(state, c, agrs, stats, refs, canManage = true, Language.En), i18n)
          _       <- I18n.let(i18n.handle)(state.setLanguage(Language.Cs))
          cs      <- renderHtml(CustomerCommercialTab.render(state, c, agrs, stats, refs, canManage = true, Language.Cs), i18n)
          _       <- I18n.let(i18n.handle)(state.setLanguage(Language.En))
          // Profile tab in edit mode renders the form; saving a tag through the state records an audit entry
          dialog  <- ConfirmDialog.Handle.make
          editRef <- Signal.initRef[Option[(CustomerId, Section)]](Some(c.id -> Section.Contacts))
          dRef    <- Signal.initRef[Option[ProfileDraft]](Some(ProfileDraft.from(c, Section.Contacts)))
          editEn  <- renderHtml(
                       CustomerProfileTab.render(state, c, custs, Some(Section.Contacts), CustomerProfileTab.Refs(editRef, dRef), dialog, canManage = true, Language.En),
                       i18n,
                     )
          ok      <- I18n.let(i18n.handle)(state.updateCustomerProfile(currentCmd(c).copy(tags = c.tags + "vip")))
          after   <- state.customers.get
        yield
          assert(en.contains("Credit usage"))
          assert(en.contains("Apply tier change"))
          assert(en.contains("Inherited from"))
          assert(en.contains("Save Payment Terms"))
          assert(cs.contains("Čerpání úvěru"))
          assert(noMissingKeys(en) && noMissingKeys(cs))
          assert(editEn.contains("mp-cust-card-editing"))
          assert(editEn.contains("Save changes"))
          assert(noMissingKeys(editEn))
          assert(ok)
          assert(after.find(_.id == c.id).exists(_.tags.contains("vip")))
      }
    }
  }
