# Architecture Documentation

## Layer Responsibilities

### Domain Layer (`domain/model/`)
Pure Kotlin data classes and enums. No Android or framework dependencies.
- `ConnectionState`, `ControllerButton`, `AnalogStick`
- `ControllerSettings`, `ConnectionHistory`
- UI state data classes

### Data Layer (`data/`)
Implementation details for persistence and network.

**Remote (`data/remote/`)**
- `TcpSocketClient` — Low-latency TCP client with auto-reconnect
- `ControllerMessage` — JSON wire format encoder

**Local (`data/local/`)**
- `PreferencesManager` — DataStore for settings and connection history

**Repository (`data/repository/`)**
- `ControllerRepository` — Single source of truth, applies deadzone/sensitivity

### Presentation Layer (`ui/`, `viewmodel/`)
- **ViewModels** expose `StateFlow` to Composables
- **Screens**: Splash, Connect, Controller, Settings
- **Components**: Reusable GameButton, DPad, VirtualAnalogStick

### DI Layer (`di/`)
- Hilt modules provide IO dispatcher and singletons

## Button Mapping (Android → Xbox 360)

| Android | Xbox 360 |
|---------|----------|
| A | A |
| B | B |
| X | X |
| Y | Y |
| START | Start |
| SELECT | Back |
| LB | Left Shoulder |
| RB | Right Shoulder |
| LT | Left Trigger (axis 0-255) |
| RT | Right Trigger (axis 0-255) |
| DPAD_UP/DOWN/LEFT/RIGHT | D-Pad |

## State Management Flow

```
User Input → Composable → ViewModel → Repository → TcpSocketClient → PC
                ↑                                      ↓
           StateFlow ← Repository ← connectionState/latencyMs
```

## Error Handling

- **Connect**: Validates IP/port format, shows error message
- **Socket**: Catches IOException, triggers auto-reconnect
- **JSON**: Server ignores malformed messages with log
- **ViGEm**: Startup catches missing driver with clear instructions
