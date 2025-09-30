# JoinMe 🤝

**Never miss out on activities again. Find partners for sports, outings, and leisure — instantly.**

[![Figma Design](https://img.shields.io/badge/Figma-Design-F24E1E?style=for-the-badge&logo=figma&logoColor=white)](https://www.figma.com/design/lUHguPMPtoEP6HFQoIMv5L/Android-App?node-id=0-1&p=f&t=7DxP8Q5WGjpctkWK-0)

---

## 📱 About JoinMe

Too often, people want to play sports or go out but can't find enough partners — it's hard to complete a soccer team, find a running buddy, or go to the movies alone. 

**JoinMe** solves this problem by letting users create and join spontaneous activities — sports, outings, leisure — geolocated around them. Users specify their interests and skill levels, and receive notifications when a relevant event is created.

### 🎯 Target Audience

- **Students** looking to socialize and meet new people
- **Young professionals** seeking partners for hobbies
- **Sports enthusiasts** who need last-minute teammates

---

## ✨ Key Features

- 📍 **Geolocation-based discovery**: Find activities happening near you
- 🔔 **Smart notifications**: Get alerted when relevant events are created
- ⚡ **Spontaneous planning**: Create and join activities in real-time
- 👤 **Personalized profiles**: Set your interests and skill levels
- 📱 **Multi-device sync**: Access your events from anywhere
- 🌐 **Offline mode**: View and prepare events even without internet

---

## 🏗️ Architecture

### Split-App Model

JoinMe relies on **Google Firebase** as its cloud backend:

| Service | Purpose |
|---------|---------|
| **Firestore** | Store events and user profiles |
| **Firebase Authentication** | Account management with Google Sign-In |
| **Firebase Cloud Messaging** | Push notifications for event updates |

These services ensure real-time synchronization and a seamless experience across devices.

### Multi-User Support

- Secure authentication through **Firebase Authentication**
- Each user logs in with their Google account
- Unique Firebase UID for each profile
- User data includes preferences, past events, and private group memberships
- Cross-device availability of all user data

---

## 📡 Sensor Usage

### GPS
Primary sensor for core functionality:
- Show nearby events based on your location
- Filter activities by distance
- Suggest partners in your area
- Set precise locations for events

### Camera
Secondary sensor for enhanced experience:
- Upload profile pictures
- Share photos of meeting spots (sports fields, cafés, etc.)

---

## 🔌 Offline Mode

JoinMe works even without an internet connection:

- ✅ View previously loaded events
- ✅ Check details of your own activities
- ✅ Prepare new events (title, location, date, etc.)
- ✅ Automatic sync when connection is restored
- ✅ No data loss

---

## 🚀 Getting Started

### Prerequisites

- Android Studio (latest version)
- Android SDK (API level XX or higher)
- Firebase account
- Google Services configuration file

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/your-org/joinme.git
   cd joinme
   ```

2. Add your `google-services.json` file to the `app/` directory

3. Open the project in Android Studio

4. Sync Gradle and build the project

5. Run on your device or emulator

---

## 👥 Team

This project is developed as part of the SwEnt course at EPFL.

- Bryan De Matos
- Saifur Mohammad Rahman
- Mathieu Arnaud Pfeffer
- Vincent Adrien Lonfat
- Atilla Altug Ülkümen
- Jeremy Kieran Zumstein
- Alexis Romain Poudens

---

## 📄 License

This project is part of an academic course at EPFL.

---

## 🤝 Contributing

This is an academic project. For any questions or suggestions, please open an issue or contact the team through our Discord channel.

