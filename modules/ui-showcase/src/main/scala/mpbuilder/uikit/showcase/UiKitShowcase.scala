package mpbuilder.uikit.showcase

import com.raquo.laminar.api.L.*
import mpbuilder.uikit.containers.*
import mpbuilder.uikit.feedback.ValidationDisplay
import mpbuilder.uikit.fields.*
import mpbuilder.uikit.form.*
import mpbuilder.uikit.util.Visibility

/** Showcase page demonstrating all ui-framework components.
  * This is a self-contained example with no domain dependencies.
  */
object UiKitShowcase:

  // ── Case class for form derivation demo ─────────────────────────────────────

  case class ContactForm(
    firstName: String,
    lastName: String,
    email: String,
    age: Int,
    subscribe: Boolean,
  )

  // ── Entry point ─────────────────────────────────────────────────────────────

  def apply(): HtmlElement =
    val activeTab = Var("fields")

    div(
      cls := "uikit-showcase",
      h1("UI Kit Showcase"),
      p("Interactive demo of all ui-framework components."),

      Tabs(
        tabs = List(
          TabDef("fields", Val("Field Components"), () => fieldComponentsSection()),
          TabDef("containers", Val("Containers"), () => containersSection()),
          TabDef("validation", Val("Validation & Visibility"), () => validationSection()),
          TabDef("derivation", Val("Form Derivation"), () => formDerivationSection()),
        ),
        activeTab = activeTab,
      ),
    )

  // ── Section 1: Field Components ─────────────────────────────────────────────

  private def fieldComponentsSection(): HtmlElement =
    val textVar     = Var("")
    val emailVar    = Var("")
    val numberVar   = Var("")
    val areaVar     = Var("")
    val selectVar   = Var("")
    val checkVar    = Var(false)
    val radioVar    = Var("")

    div(
      cls := "showcase-section",
      h2("Field Components"),

      h3("TextField"),
      div(
        cls := "showcase-row",
        TextField(
          label = Val("Name"),
          value = textVar.signal,
          onInput = textVar.writer,
          placeholder = Val("Enter your name"),
        ),
        TextField(
          label = Val("Email"),
          value = emailVar.signal,
          onInput = emailVar.writer,
          inputType = "email",
          placeholder = Val("user@example.com"),
        ),
        TextField(
          label = Val("Age"),
          value = numberVar.signal,
          onInput = numberVar.writer,
          inputType = "number",
          placeholder = Val("25"),
        ),
      ),
      div(
        cls := "showcase-output",
        child.text <-- textVar.signal.combineWith(emailVar.signal, numberVar.signal).map {
          case (name, email, age) => s"Name: $name | Email: $email | Age: $age"
        },
      ),

      h3("TextField with Validation Error"),
      {
        val errorField = FormFieldState[String]("username", FieldValidator.required, FieldValidator.minLength(3))
        div(
          cls := "showcase-row",
          TextField(
            label = Val("Username (required, min 3 chars)"),
            value = errorField.rawVar.signal,
            onInput = errorField.touchedWriter,
            placeholder = Val("Type something short then clear it"),
            error = errorField.firstError,
          ),
          div(
            cls := "showcase-output",
            child.text <-- errorField.parsed.map {
              case Right(v)    => s"Valid: \"$v\""
              case Left(errs)  => s"Errors: ${errs.mkString(", ")}"
            },
          ),
        )
      },

      h3("TextAreaField"),
      TextAreaField(
        label = Val("Description"),
        value = areaVar.signal,
        onInput = areaVar.writer,
        placeholder = Val("Write a longer text here..."),
      ),

      h3("SelectField"),
      SelectField(
        label = Val("Country"),
        options = Val(List(
          SelectOption("cz", "Czech Republic"),
          SelectOption("sk", "Slovakia"),
          SelectOption("de", "Germany"),
          SelectOption("at", "Austria"),
        )),
        selected = selectVar.signal,
        onChange = selectVar.writer,
        placeholder = Val("-- Choose a country --"),
      ),
      div(cls := "showcase-output", child.text <-- selectVar.signal.map(v => s"Selected: $v")),

      h3("CheckboxField"),
      CheckboxField(
        label = Val("I agree to the terms and conditions"),
        checked = checkVar.signal,
        onChange = checkVar.writer,
      ),
      div(cls := "showcase-output", child.text <-- checkVar.signal.map(v => s"Checked: $v")),

      h3("RadioGroup"),
      RadioGroup(
        label = Val("Preferred contact method"),
        name = "contact-method",
        options = Val(List(
          RadioOption("email", Val("Email")),
          RadioOption("phone", Val("Phone")),
          RadioOption("mail", Val("Postal Mail")),
        )),
        selected = radioVar.signal,
        onChange = radioVar.writer,
      ),
      div(cls := "showcase-output", child.text <-- radioVar.signal.map(v => s"Selected: $v")),
    )

  // ── Section 2: Containers ───────────────────────────────────────────────────

  private def containersSection(): HtmlElement =
    val innerTab = Var("tab-a")
    val stepVar  = Var("step-1")

    div(
      cls := "showcase-section",
      h2("Container Components"),

      h3("Tabs"),
      Tabs(
        tabs = List(
          TabDef("tab-a", Val("Alpha"), () => div(p("Content of the Alpha tab."))),
          TabDef("tab-b", Val("Beta"), () => div(p("Content of the Beta tab."))),
          TabDef("tab-c", Val("Gamma"), () => div(p("Content of the Gamma tab."))),
        ),
        activeTab = innerTab,
      ),

      h3("Stepper"),
      Stepper(
        steps = List(
          StepDef("step-1", Val("Details"), () => div(
            p("Step 1: Fill in your details."),
            TextField(label = Val("Full name"), value = Val(""), onInput = Observer.empty),
          )),
          StepDef("step-2", Val("Review"), () => div(
            p("Step 2: Review your information."),
          )),
          StepDef("step-3", Val("Confirm"), () => div(
            p("Step 3: Confirm and submit."),
            button("Submit (no-op)", cls := "checkout-btn"),
          )),
        ),
        currentStep = stepVar,
      ),
    )

  // ── Section 3: Validation & Visibility ──────────────────────────────────────

  private def validationSection(): HtmlElement =
    val showBox = Var(true)
    val errorsVar: Var[List[String]] = Var(Nil)
    val successVar: Var[Option[String]] = Var(None)

    div(
      cls := "showcase-section",
      h2("Validation & Visibility"),

      h3("ValidationDisplay"),
      div(
        cls := "showcase-row",
        button("Add error", onClick --> { _ =>
          val n = errorsVar.now().size + 1
          errorsVar.update(_ :+ s"Validation error #$n")
          successVar.set(None)
        }),
        button("Set success", onClick --> { _ =>
          errorsVar.set(Nil)
          successVar.set(Some("All validations passed!"))
        }),
        button("Clear", onClick --> { _ =>
          errorsVar.set(Nil)
          successVar.set(None)
        }),
      ),
      ValidationDisplay(
        errors = errorsVar.signal,
        success = successVar.signal,
      ),

      h3("Visibility.when / Visibility.unless"),
      CheckboxField(
        label = Val("Show the box below"),
        checked = showBox.signal,
        onChange = showBox.writer,
      ),
      div(
        Visibility.when(showBox.signal),
        cls := "showcase-visibility-box",
        p("This box is controlled by Visibility.when(signal)."),
      ),
      div(
        Visibility.unless(showBox.signal),
        cls := "showcase-visibility-box",
        p("This box is controlled by Visibility.unless(signal) — visible when the checkbox is OFF."),
      ),
    )

  // ── Section 4: Form Derivation ──────────────────────────────────────────────

  private def formDerivationSection(): HtmlElement =
    // Derive form state from case class at compile time
    val form = FormState.create[ContactForm]
      .withValidators[String]("firstName", FieldValidator.required, FieldValidator.minLength(2))
      .withValidators[String]("lastName", FieldValidator.required)
      .withValidators[String]("email", FieldValidator.required, FieldValidator.regex("^[^@]+@[^@]+\\.[^@]+$", "Must be a valid email"))
      .withValidators[Int]("age", FieldValidator.min(1), FieldValidator.max(150))

    div(
      cls := "showcase-section",
      h2("Form Derivation"),

      p("This form is auto-derived from a case class using ", code("FormState.create[ContactForm]"), ". ",
        "Field names are humanized automatically. Validators are attached via ", code(".withValidators"), "."),

      pre(cls := "showcase-code",
        """case class ContactForm(
          |  firstName: String,
          |  lastName: String,
          |  email: String,
          |  age: Int,
          |  subscribe: Boolean,
          |)
          |
          |val form = FormState.create[ContactForm]
          |  .withValidators[String]("firstName", FieldValidator.required, FieldValidator.minLength(2))
          |  .withValidators[String]("lastName", FieldValidator.required)
          |  .withValidators[String]("email", FieldValidator.required, FieldValidator.regex(...))
          |  .withValidators[Int]("age", FieldValidator.min(1), FieldValidator.max(150))
          |
          |val element = FormRenderer.render(form, Map(
          |  "email" -> FieldConfig(inputType = Some("email")),
          |))
          |val validated: Signal[Either[Map[String, List[String]], ContactForm]] = form.validated""".stripMargin
      ),

      h3("Rendered Form"),
      FormRenderer.render(form, Map(
        "firstName" -> FieldConfig(placeholder = Some(Val("John"))),
        "lastName"  -> FieldConfig(placeholder = Some(Val("Smith"))),
        "email"     -> FieldConfig(inputType = Some("email"), placeholder = Some(Val("john@example.com"))),
        "age"       -> FieldConfig(placeholder = Some(Val("25"))),
      )),

      h3("Live Validated Output"),
      div(
        cls := "showcase-row",
        button("Touch All Fields", onClick --> { _ => form.touchAll() }),
      ),

      // Show validation errors
      ValidationDisplay(
        errors = form.allErrors,
      ),

      // Show validated output
      div(
        cls := "showcase-output",
        child.text <-- form.validated.map {
          case Right(contact) =>
            s"Valid: ContactForm(${contact.firstName}, ${contact.lastName}, ${contact.email}, ${contact.age}, ${contact.subscribe})"
          case Left(errors) =>
            val msgs = errors.map { case (field, errs) => s"$field: ${errs.mkString(", ")}" }
            s"Invalid: ${msgs.mkString(" | ")}"
        },
      ),
    )
