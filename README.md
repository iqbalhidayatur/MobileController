Mobile Game Controller

Aplikasi Android yang mengubah smartphone menjadi virtual game controller (Xbox-style) untuk PC Windows melalui WiFi lokal. Input dari ponsel dikirim via TCP Socket dan di PC diubah menjadi Virtual Xbox 360 Controller menggunakan ViGEmBus.



Struktur Project

MobileGameController/
├── android/                          # Aplikasi Android (Kotlin + Compose)
│   ├── app/
│   │   └── src/main/java/com/mobilegamecontroller/
│   │       ├── data/                 # Data layer (remote, local, repository)
│   │       ├── domain/model/         # Domain models
│   │       ├── di/                   # Hilt dependency injection
│   │       ├── ui/                   # Compose UI (screens, components, theme)
│   │       └── viewmodel/            # MVVM ViewModels
│   └── build.gradle.kts
├── windows-server/                   # Server PC (C# .NET 8)
│   └── MobileGameControllerServer/
│       ├── Program.cs
│       ├── ControllerTcpServer.cs
│       ├── VirtualControllerManager.cs
│       └── Models/
├── docs/
│   └── ARCHITECTURE.md
└── README.md



Diagram Arsitektur Sistem

flowchart TB
    subgraph Android["📱 Android App"]
        UI["Jetpack Compose UI"]
        VM["ViewModel (MVVM)"]
        REPO["Repository"]
        TCP_CLIENT["TcpSocketClient"]
        PREFS["PreferencesManager"]
        UI --> VM --> REPO
        REPO --> TCP_CLIENT
        REPO --> PREFS
    end

    subgraph Network["🌐 WiFi LAN"]
        TCP["TCP Socket\nPort 27015\nJSON + Newline"]
    end

    subgraph Windows["🖥️ Windows PC"]
        TCP_SERVER["ControllerTcpServer"]
        VIGEM["VirtualControllerManager"]
        XBOX["Virtual Xbox 360\n(ViGEmBus Driver)"]
        GAME["Windows Game"]
        TCP_SERVER --> VIGEM --> XBOX --> GAME
    end

    TCP_CLIENT <-->|"Real-time JSON"| TCP
    TCP <-->|"Real-time JSON"| TCP_SERVER



Diagram Komunikasi Android → PC

sequenceDiagram
    participant User as Pengguna
    participant App as Android App
    participant TCP as TCP Socket
    participant Server as PC Server
    participant ViGEm as ViGEmBus
    participant Game as Game Windows

    User->>App: Tekan tombol A
    App->>App: Encode JSON
    Note over App: {"type":"button","button":"A","pressed":true}
    App->>TCP: Send + flush (TCP_NODELAY)
    TCP->>Server: Receive line
    Server->>ViGEm: SetButtonState(A, true)
    ViGEm->>Game: Xbox 360 input event

    User->>App: Gerakkan analog kiri
    App->>App: Apply deadzone + sensitivity
    Note over App: {"type":"analog","stick":"left","x":0.75,"y":-0.32}
    App->>TCP: Send analog data
    TCP->>Server: Receive line
    Server->>ViGEm: SetAxisValue(LeftThumbX/Y)
    ViGEm->>Game: Stick movement

    loop Every 2 seconds
        App->>Server: {"type":"ping","timestamp":...}
        Server->>App: {"type":"pong","timestamp":...}
        App->>App: Calculate latency (RTT)
    end



Protokol Data (JSON)

Setiap pesan adalah satu objek JSON diakhiri newline (\n).

Button press/release:

{"type":"button","button":"A","pressed":true}
{"type":"button","button":"A","pressed":false}

Analog stick (real-time):

{"type":"analog","stick":"left","x":0.75,"y":-0.32}
{"type":"analog","stick":"right","x":0.0,"y":0.0}

Ping/Pong (latency):

{"type":"ping","timestamp":1718361600000}
{"type":"pong","timestamp":1718361600000}

Tombol yang didukung: A, B, X, Y, START, SELECT, LB, RB, LT, RT, DPAD_UP, DPAD_DOWN, DPAD_LEFT, DPAD_RIGHT



Instalasi

Prasyarat Windows (PC Server)





Windows 10/11 (64-bit)



.NET 8 SDK



ViGEmBus Driver — wajib diinstall agar game mendeteksi controller virtual



Smartphone dan PC harus berada di jaringan WiFi yang sama

Prasyarat Android





Android Studio (Ladybug atau lebih baru)



JDK 17



Perangkat Android API 24+ (Android 7.0+)



Cara Menjalankan

1. Install ViGEmBus (sekali saja)





Download installer dari ViGEmBus Releases



Jalankan installer sebagai Administrator



Restart PC jika diminta

2. Jalankan Server Windows

cd C:\Users\Administrator\Projects\MobileGameController\windows-server\MobileGameControllerServer

# Restore & build
dotnet restore
dotnet build

# Jalankan server (default port 27015)
dotnet run

# Atau dengan port custom:
dotnet run -- 8080

Server akan menampilkan alamat IP lokal PC, contoh:

→ 192.168.1.100:27015

Firewall: Izinkan inbound TCP pada port 27015:

New-NetFirewallRule -DisplayName "Mobile Game Controller" -Direction Inbound -Protocol TCP -LocalPort 27015 -Action Allow

3. Build & Install Aplikasi Android





Buka folder android/ di Android Studio



Sync Gradle



Connect perangkat Android via USB (aktifkan USB Debugging)



Run ▶ (atau ./gradlew installDebug)

4. Connect & Play





Buka Mobile Game Controller di ponsel



Masukkan IP Address PC (dari output server)



Masukkan Port (default: 27015)



Tap Connect



Setelah status Connected, gunakan controller



Buka game di PC — controller Xbox 360 virtual akan terdeteksi otomatis



Fitur Aplikasi Android







Fitur



Deskripsi





D-Pad



Up, Down, Left, Right





Face buttons



A, B, X, Y





Menu



Start, Select





Shoulders



LB, RB, LT, RT





Analog sticks



Kiri & kanan, 360°





Haptic feedback



Getaran saat tombol ditekan





Auto reconnect



Reconnect otomatis saat koneksi putus





Latency indicator



Ping RTT dalam ms





Settings



Sensitivitas, deadzone, tema, getaran





Connection history



5 koneksi terakhir



Arsitektur Android (Clean Architecture + MVVM)

┌─────────────────────────────────────────┐
│              UI Layer                    │
│  Screens → Components → Navigation       │
├─────────────────────────────────────────┤
│           ViewModel Layer                │
│  ConnectViewModel, ControllerViewModel   │
├─────────────────────────────────────────┤
│          Repository Layer                │
│  ControllerRepository (single source)    │
├─────────────────────────────────────────┤
│            Data Layer                    │
│  TcpSocketClient | PreferencesManager    │
└─────────────────────────────────────────┘





Hilt — Dependency Injection



StateFlow — Reactive UI state



Coroutines — Async network I/O



DataStore — Persistent settings



kotlinx.serialization — JSON encoding



Best Practice: Optimasi Performa & Latensi Rendah

Jaringan







Teknik



Implementasi





TCP_NODELAY



Nonaktifkan Nagle's algorithm di Android (socket.tcpNoDelay = true) dan PC (client.NoDelay = true)





Immediate flush



Button events di-flush langsung; analog juga di-flush untuk responsivitas maksimal





Compact JSON



Tanpa pretty-print; payload minimal (~40-80 bytes per event)





Newline framing



Simple delimiter tanpa overhead protocol header





Keep-alive



socket.keepAlive = true untuk deteksi disconnect cepat

Android







Teknik



Implementasi





IO Dispatcher



Semua network I/O di Dispatchers.IO, tidak pernah di Main thread





Landscape lock



screenOrientation="sensorLandscape" untuk layout controller optimal





Immersive mode



Fullscreen tanpa system bars saat bermain





Deadzone client-side



Filter noise analog sebelum kirim, kurangi traffic





Exponential backoff



Auto reconnect: 1s → 2s → 4s → max 8s

Windows Server







Teknik



Implementasi





ViGEm SubmitReport



Kirim report segera setelah setiap perubahan state





Per-client handler



Async task per koneksi, non-blocking accept loop





Controller reset



Reset semua input saat client disconnect





Buffer size



4KB send/receive buffer — cukup untuk JSON lines

Tips Pengguna





Gunakan WiFi 5GHz jika memungkinkan (latensi lebih rendah dari 2.4GHz)



Pastikan PC dan ponsel di subnet yang sama (e.g. 192.168.1.x)



Tutup aplikasi bandwidth-heavy di jaringan yang sama



Latensi normal di LAN: 5-30ms



Troubleshooting







Masalah



Solusi





Connection failed



Cek IP, port, firewall, same WiFi network





ViGEmBus not found



Install driver dari GitHub releases, restart PC





Game tidak deteksi controller



Pastikan server running dan client connected





Input delay tinggi



Gunakan 5GHz WiFi, dekatkan ke router





Auto reconnect loop



Restart server, cek koneksi jaringan



Lisensi

Project ini dibuat untuk keperluan edukasi dan penggunaan pribadi.

