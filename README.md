🌈 LED Strip Controller

Projekt składa się z kontrolera LED opartego o Arduino UNO R4 WiFi oraz aplikacji na Androida, która pozwala sterować taśmą LED poprzez Bluetooth Low Energy (BLE).

Dzięki temu zestawowi możesz w prosty sposób:

zmieniać kolor świecenia taśmy LED 🎨

regulować jasność 💡

włączyć tryb tęczy 🌈

wyłączyć podświetlenie jednym przyciskiem ⭕

⚡ Hardware (Arduino)

Kontroler oparty jest na:

Arduino UNO R4 WiFi (obsługa BLE)

taśmie LED WS2812B / Neopixel (do 300 diod)

bibliotece Adafruit NeoPixel do sterowania LED

bibliotece ArduinoBLE do komunikacji BLE

Arduino wystawia usługę BLE z charakterystykami, które aplikacja mobilna wykorzystuje do wysyłania poleceń:

rainbow → włącza animację tęczy

color,R,G,B → ustawia statyczny kolor

0–255 → zmienia jasność

color,0,0,0 → wyłącza diody

📱 Aplikacja na Androida

Aplikacja została napisana w Kotlinie z użyciem Jetpack Compose i działa na Androidzie 6.0+.

Funkcjonalności:

automatyczne wyszukiwanie urządzenia BLE o nazwie LED STRIP

suwak do regulacji jasności

ColorPicker do wyboru koloru (zapisywanie ustawień w DataStore)

przyciski szybkiego wyboru: 🌈 Rainbow, ⭕ Off

zapamiętywanie ostatnich ustawień po ponownym uruchomieniu

Wymagane uprawnienia:

Bluetooth / Bluetooth LE

(na starszych Androidach) dostęp do lokalizacji

🔧 Jak uruchomić?

Wgraj kod Arduino na UNO R4 WiFi.

Podłącz taśmę LED do pinu D2 (i odpowiednie zasilanie).

Odpal aplikację na telefonie z Androidem.

Połącz się z urządzeniem LED STRIP.

Steruj kolorami, jasnością i trybami! 🚀
