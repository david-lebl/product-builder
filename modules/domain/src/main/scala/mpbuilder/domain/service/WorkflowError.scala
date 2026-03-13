package mpbuilder.domain.service

import mpbuilder.domain.model.*

/** Error ADT for workflow engine state transitions. */
enum WorkflowError:
  case StepNotFound(stepId: StepId)
  case StepNotReady(stepId: StepId, currentStatus: StepStatus)
  case StepNotInProgress(stepId: StepId, currentStatus: StepStatus)
  case StepAlreadyCompleted(stepId: StepId)
  case StepAlreadySkipped(stepId: StepId)
  case DependenciesNotMet(stepId: StepId, unmetDependencies: Set[StepId])
  case WorkflowNotActive(workflowId: WorkflowId, currentStatus: WorkflowStatus)
  case StepCannotBeSkipped(stepId: StepId, stationType: StationType)
  case StepCannotBeReset(stepId: StepId, currentStatus: StepStatus)

  def message: String = message(Language.En)

  def message(lang: Language): String = this match
    case StepNotFound(stepId) => lang match
      case Language.En => s"Step '${stepId.value}' not found in workflow"
      case Language.Cs => s"Krok '${stepId.value}' nebyl nalezen v pracovním postupu"
    case StepNotReady(stepId, status) => lang match
      case Language.En => s"Step '${stepId.value}' is not ready (current status: $status)"
      case Language.Cs => s"Krok '${stepId.value}' není připraven (aktuální stav: $status)"
    case StepNotInProgress(stepId, status) => lang match
      case Language.En => s"Step '${stepId.value}' is not in progress (current status: $status)"
      case Language.Cs => s"Krok '${stepId.value}' není rozpracován (aktuální stav: $status)"
    case StepAlreadyCompleted(stepId) => lang match
      case Language.En => s"Step '${stepId.value}' is already completed"
      case Language.Cs => s"Krok '${stepId.value}' je již dokončen"
    case StepAlreadySkipped(stepId) => lang match
      case Language.En => s"Step '${stepId.value}' is already skipped"
      case Language.Cs => s"Krok '${stepId.value}' je již přeskočen"
    case DependenciesNotMet(stepId, unmet) => lang match
      case Language.En => s"Step '${stepId.value}' has unmet dependencies: ${unmet.map(_.value).mkString(", ")}"
      case Language.Cs => s"Krok '${stepId.value}' má nesplněné závislosti: ${unmet.map(_.value).mkString(", ")}"
    case WorkflowNotActive(wfId, status) => lang match
      case Language.En => s"Workflow '${wfId.value}' is not active (current status: $status)"
      case Language.Cs => s"Pracovní postup '${wfId.value}' není aktivní (aktuální stav: $status)"
    case StepCannotBeSkipped(stepId, stationType) => lang match
      case Language.En => s"Step '${stepId.value}' ($stationType) cannot be skipped — it is a required station"
      case Language.Cs => s"Krok '${stepId.value}' ($stationType) nelze přeskočit — je to povinná stanice"
    case StepCannotBeReset(stepId, status) => lang match
      case Language.En => s"Step '${stepId.value}' cannot be reset (current status: $status)"
      case Language.Cs => s"Krok '${stepId.value}' nelze resetovat (aktuální stav: $status)"
