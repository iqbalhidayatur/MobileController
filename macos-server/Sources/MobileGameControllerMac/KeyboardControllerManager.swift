import AppKit
import ApplicationServices
import Carbon.HIToolbox
import Foundation

/// Forwards controller events to keyboard and mouse using CGEvent.
/// macOS has no public virtual gamepad API like ViGEm on Windows.
final class KeyboardControllerManager: @unchecked Sendable {
    private let queue = DispatchQueue(label: "KeyboardControllerManager")
    private var pressedKeys = Set<CGKeyCode>()
    private var leftStickKeys = Set<CGKeyCode>()
    private let analogDeadzone: Float = 0.12
    private let mouseSensitivity: CGFloat = 18

    private enum KeyBinding {
        static let mapping: [String: CGKeyCode] = [
            "A": CGKeyCode(kVK_Space),
            "B": CGKeyCode(kVK_Shift),
            "X": CGKeyCode(kVK_ANSI_E),
            "Y": CGKeyCode(kVK_ANSI_Q),
            "START": CGKeyCode(kVK_Return),
            "SELECT": CGKeyCode(kVK_Escape),
            "LB": CGKeyCode(kVK_ANSI_1),
            "RB": CGKeyCode(kVK_ANSI_2),
            "LT": CGKeyCode(kVK_ANSI_3),
            "RT": CGKeyCode(kVK_ANSI_4),
            "DPAD_UP": CGKeyCode(kVK_UpArrow),
            "DPAD_DOWN": CGKeyCode(kVK_DownArrow),
            "DPAD_LEFT": CGKeyCode(kVK_LeftArrow),
            "DPAD_RIGHT": CGKeyCode(kVK_RightArrow)
        ]

        static let leftStickUp = CGKeyCode(kVK_ANSI_W)
        static let leftStickDown = CGKeyCode(kVK_ANSI_S)
        static let leftStickLeft = CGKeyCode(kVK_ANSI_A)
        static let leftStickRight = CGKeyCode(kVK_ANSI_D)
    }

    static func ensureAccessibilityPermission() -> Bool {
        let options = [kAXTrustedCheckOptionPrompt.takeUnretainedValue() as String: true] as CFDictionary
        return AXIsProcessTrustedWithOptions(options)
    }

    func handleButton(_ button: String, pressed: Bool) {
        queue.async {
            guard let keyCode = KeyBinding.mapping[button.uppercased()] else { return }
            self.setKey(keyCode, pressed: pressed)
        }
    }

    func handleTrigger(_ button: String, pressed: Bool) {
        handleButton(button, pressed: pressed)
    }

    func handleAnalog(stick: String, x: Float, y: Float) {
        queue.async {
            switch stick.lowercased() {
            case "left":
                self.updateLeftStick(x: x, y: y)
            case "right":
                self.moveMouse(x: x, y: y)
            default:
                break
            }
        }
    }

    func resetController() {
        queue.async {
            for keyCode in self.pressedKeys {
                self.postKey(keyCode, keyDown: false)
            }
            self.pressedKeys.removeAll()

            for keyCode in self.leftStickKeys {
                self.postKey(keyCode, keyDown: false)
            }
            self.leftStickKeys.removeAll()
        }
    }

    private func updateLeftStick(x: Float, y: Float) {
        let desiredKeys: Set<CGKeyCode> = [
            y > analogDeadzone ? KeyBinding.leftStickUp : nil,
            y < -analogDeadzone ? KeyBinding.leftStickDown : nil,
            x < -analogDeadzone ? KeyBinding.leftStickLeft : nil,
            x > analogDeadzone ? KeyBinding.leftStickRight : nil
        ].compactMap { $0 }.reduce(into: Set<CGKeyCode>()) { $0.insert($1) }

        for key in leftStickKeys.subtracting(desiredKeys) {
            postKey(key, keyDown: false)
        }
        for key in desiredKeys.subtracting(leftStickKeys) {
            postKey(key, keyDown: true)
        }

        leftStickKeys = desiredKeys
    }

    private func moveMouse(x: Float, y: Float) {
        let magnitude = hypotf(x, y)
        guard magnitude > analogDeadzone else { return }

        let dx = CGFloat(x) * mouseSensitivity
        let dy = CGFloat(-y) * mouseSensitivity

        guard let currentEvent = CGEvent(source: nil) else { return }
        let current = currentEvent.location
        let destination = CGPoint(x: current.x + dx, y: current.y + dy)

        guard let moveEvent = CGEvent(
            mouseEventSource: nil,
            mouseType: .mouseMoved,
            mouseCursorPosition: destination,
            mouseButton: .left
        ) else { return }

        moveEvent.post(tap: .cghidEventTap)
    }

    private func setKey(_ keyCode: CGKeyCode, pressed: Bool) {
        if pressed {
            guard !pressedKeys.contains(keyCode) else { return }
            pressedKeys.insert(keyCode)
            postKey(keyCode, keyDown: true)
        } else {
            guard pressedKeys.contains(keyCode) else { return }
            pressedKeys.remove(keyCode)
            postKey(keyCode, keyDown: false)
        }
    }

    private func postKey(_ keyCode: CGKeyCode, keyDown: Bool) {
        guard let event = CGEvent(
            keyboardEventSource: nil,
            virtualKey: keyCode,
            keyDown: keyDown
        ) else { return }

        event.post(tap: .cghidEventTap)
    }
}
