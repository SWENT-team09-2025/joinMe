# Firestore schema – profiles collection

Collection: profiles
Document ID: user.uid

Fields:
- username (string, required)
- email (string, required)
- photoUrl (string, optional)
- country (string, optional)
- bio (string, optional)
- interests (array<string>, optional)
- createdAt (timestamp, serverTimestamp)
- updatedAt (timestamp, serverTimestamp)

Indexes: none (client queries only by uid)

Relations:
- Auth.uid == document.id