# JoinMe

**Never miss out on activities again. Find partners for sports, outings, and leisure — instantly.**

[![Figma Design](https://img.shields.io/badge/Figma-Design-F24E1E?style=for-the-badge&logo=figma&logoColor=white)](https://www.figma.com/design/lUHguPMPtoEP6HFQoIMv5L/Android-App?node-id=0-1&p=f&t=7DxP8Q5WGjpctkWK-0)

---

## About JoinMe

We've all been there — you want to play a game of soccer but can't find enough players, you're craving a hike but none of your friends are available, or you'd love to check out that new café but going alone feels awkward.

**JoinMe** is your solution. Our app connects you with like-minded people nearby who share your interests and are ready to join you — right now. Whether it's sports, social outings, or spontaneous adventures, JoinMe turns "I wish I could" into "Let's go!"

Create an activity, set your location, and watch as others join in. Or browse what's happening around you and jump into something new. It's that simple.

### Target Audience

- **Students** — Meet new people, break out of your routine, and make university life unforgettable
- **Young professionals** — Balance work with play by finding hobby partners who match your schedule
- **Sports enthusiasts** — Never cancel a game again because you're short on players

---

## Features

### Core Experience

- **Geolocation-based discovery** — Find activities happening near you on an interactive map
- **Smart filtering** — Filter events by activity type on both search and map screens
- **Spontaneous planning** — Create and join activities in real-time
- **Push notifications** — Get alerted when relevant events are created

### Social and Community

- **Groups** — Create or join communities around shared interests with custom group pictures
- **Direct messaging** — Chat with other users, share photos and locations
- **Rich profiles** — View activity history, follower/following counts, and browse user connections
- **Follow system** — Follow users and build your network within the app
- **Group leaderboards** — Track engagement with Current and All Time rankings
- **Streak system** — Maintain weekly activity streaks within your groups

### Events and Series

- **Calendar view** — Browse and manage your upcoming activities
- **Event series** — Organize events into series for related activities
- **Invitation links** — Share deep links for events, series, or groups that open directly in the app

### Chat Features

- **Real-time messaging** — Instant communication powered by Firebase Realtime Database
- **Photo sharing** — Send images directly in conversations
- **Location sharing** — Share your position or meeting points with other users
- **Polls** — Create polls to help groups make decisions together

### Reliability

- **Multi-device sync** — Access your events from anywhere
- **Offline mode** — View events, calendar, groups, and chat history even without internet

---

## Architecture

### Technical Stack

JoinMe is built with modern Android development practices:

- **Kotlin** with **Jetpack Compose** for a declarative UI
- **MVVM architecture** with clean separation of concerns
- **Room database** for robust offline-first data persistence
- **Firebase** suite for cloud backend services

### Firebase Integration

| Service | Purpose |
|---------|---------|
| Firestore | Storage for events, users, series, and groups |
| Realtime Database | Real-time chat messaging and synchronization |
| Firebase Authentication | Secure account management with Google Sign-In |
| Cloud Storage | Profile pictures, group images, and chat attachments |
| Cloud Messaging | Push notifications for event updates and messages |
| Dynamic Links | Deep linking for invitation sharing |

### Multi-User Support

- Secure authentication through Firebase Authentication
- Google account integration with unique Firebase UID per user
- User data includes preferences, activity history, group memberships, and social connections
- Synchronization across all devices

---

## Sensor Usage

### GPS

Primary sensor for core functionality:

- Display nearby events based on current location
- Filter activities by distance
- Set precise locations when creating events
- Navigate to event locations via map integration

### Camera

Secondary sensor for enhanced experience:

- Upload and update profile pictures
- Share photos in chat conversations
- Capture images of meeting spots and venues

---

## Offline Mode

JoinMe provides a seamless experience even without internet connectivity:

- Browse previously loaded events and event details
- View your calendar and upcoming activities
- Access group information and chat history
- Automatic synchronization when connection is restored

---

## Getting Started

### Prerequisites

- Android Studio (latest stable version)
- Firebase project with required services enabled
- Google Services configuration file

### Installation

1. Clone the repository:
```bash
   git clone https://github.com/SWENT-team09-2025/joinMe.git
   cd joinMe
```

2. Add your `google-services.json` file to the `app/` directory

3. Open the project in Android Studio

4. Sync Gradle and build the project

5. Run on your device or emulator

---

## Team

This project is developed as part of the Software Enterprise (SwEnt) course at EPFL.

| Name | Role |
|------|------|
| Bryan De Matos | Developer |
| Saifur Mohammad Rahman | Developer |
| Mathieu Arnaud Pfeffer | Developer |
| Vincent Adrien Lonfat | Developer |
| Atilla Altug Ülkümen | Developer |
| Jeremy Kieran Zumstein | Developer |
| Alexis Romain Poudens | Developer |

---

## License

This project is part of an academic course at EPFL.

---

## Contributing

This is an academic project. For questions or suggestions, please open an issue or contact the team.
