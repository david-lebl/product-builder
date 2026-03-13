package mpbuilder.domain.service

import mpbuilder.domain.model.*

/** Error ADT for employee and machine management operations. */
enum ManagementError:
  case EmployeeNotFound(employeeId: EmployeeId)
  case EmployeeNameEmpty
  case EmployeeAlreadyExists(employeeId: EmployeeId)
  case EmployeeNoCapabilities
  case MachineNotFound(machineId: MachineId)
  case MachineNameEmpty
  case MachineAlreadyExists(machineId: MachineId)

  def message: String = message(Language.En)

  def message(lang: Language): String = this match
    case EmployeeNotFound(id) => lang match
      case Language.En => s"Employee '${id.value}' not found"
      case Language.Cs => s"Zaměstnanec '${id.value}' nebyl nalezen"
    case EmployeeNameEmpty => lang match
      case Language.En => "Employee name must not be empty"
      case Language.Cs => "Jméno zaměstnance nesmí být prázdné"
    case EmployeeAlreadyExists(id) => lang match
      case Language.En => s"Employee '${id.value}' already exists"
      case Language.Cs => s"Zaměstnanec '${id.value}' již existuje"
    case EmployeeNoCapabilities => lang match
      case Language.En => "Employee must have at least one station capability"
      case Language.Cs => "Zaměstnanec musí mít alespoň jednu stanicovou dovednost"
    case MachineNotFound(id) => lang match
      case Language.En => s"Machine '${id.value}' not found"
      case Language.Cs => s"Stroj '${id.value}' nebyl nalezen"
    case MachineNameEmpty => lang match
      case Language.En => "Machine name must not be empty"
      case Language.Cs => "Název stroje nesmí být prázdný"
    case MachineAlreadyExists(id) => lang match
      case Language.En => s"Machine '${id.value}' already exists"
      case Language.Cs => s"Stroj '${id.value}' již existuje"
