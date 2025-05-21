# safewomen App Documentation 
##Overview


SafeWomen is an Android application designed to enhance personal safety by providing emergency contact management, real-time location tracking, and quick access to SOS features. The app is built using the MVVM (Model-View-ViewModel) architecture, Room for local storage, and integrates with a PHP/MySQL backend for user authentication and data synchronization.


##Main Features


User Authentication: Register, login, and manage user sessions securely.
Emergency Contacts: Add, edit, delete, and import emergency contacts from the phone.
Location Tracking: Real-time GPS tracking with history and safety zone visualization.
SOS Alerts: Send emergency SMS and initiate calls to contacts with location info.
User Settings: Manage notification preferences, dark mode, and other settings.
Offline Support: Local Room database for emergency contacts and location history.


Architecture

1.MVVM Pattern

Model: Data classes (e.g., UserEntity, EmergencyContactEntity, LocationHistoryEntity), Room database, and repositories.
ViewModel: Business logic and LiveData for UI updates (e.g., AuthViewModel, EmergencyContactsViewModel, MapViewModel).
View: Activities and Fragments (e.g., MainActivity, ContactFragment, MapFragment), using View Binding for UI.

2. Room Database

Stores user, emergency contacts, location history, and user settings locally.
Entities: UserEntity, EmergencyContactEntity, LocationHistoryEntity, UserSettingsEntity.

3. Repositories

Abstract data operations and handle synchronization with the backend.
Examples: AuthRepository, ContactRepository, LocationHistoryRepository.

4. LiveData & ViewModel

All UI data is exposed via LiveData for reactive updates.
ViewModels handle business logic and interact with repositories.


Backend Integration

PHP/MySQL backend for user authentication, contact sync, and location data.
API endpoints for login, registration, contact CRUD, and location upload.
Models in PHP mirror the Android entities (see previous answers for code).

Location Tracking & Map

Uses Google Maps SDK.
Real-time tracking via foreground service.
Location history displayed as markers.
Safety zones visualized as colored circles.
Share and navigate to locations.

User Settings

Notification preferences.
SOS trigger method.
Auto location sharing.
Dark mode toggle.
Emergency message template.
Accessibility & UX

Material Design components.

Accessible dialogs and touch targets.
Snackbar for feedback.
View Binding for type-safe UI access.

How to Build & Run

Clone the repository.
Open in Android Studio.
Configure Google Maps API key in AndroidManifest.xml.
Set backend API URL in your repository or constants.
Build and run on a device or emulator.
(Optional) Set up the PHP backend and MySQL database as described in previous answers.

Contact

For questions, issues, or contributions, please contact yasserrabai@gmail.com
