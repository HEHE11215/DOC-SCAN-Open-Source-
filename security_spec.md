# Firestore Security Specification (DocuScan)

## 1. Data Invariants
1. **Strict Multi-User Isolation**: Every document in `/users/{userId}/documents/{docId}` and `/users/{userId}/folders/{folderId}` belongs exclusively to `userId`. Only an authenticated user whose `request.auth.uid == userId` may read, list, create, update, or delete records in their subcollections.
2. **Identity Integrity**: The payload's `userId` field must equal both `request.auth.uid` and the path parameter `{userId}`, and must remain immutable across updates (`incoming().userId == existing().userId`).
3. **Temporal Integrity**: `createdAt` and `updatedAt` must be valid Firestore `timestamp` objects bounded by `<= request.time`. On update, `createdAt` must be strictly immutable (`incoming().createdAt == existing().createdAt`).
4. **Bounded Strings & Collections**:
   - `docId` and `folderId` must match `^[a-zA-Z0-9_\-]+$` and be `<= 128` chars.
   - `title`: `1..200` chars.
   - `ocrText`: `0..100000` chars.
   - `aiSummary`: `0..20000` chars.
   - `tags`: optional list of at most 10 strings, first element checked as string `<= 50` chars.
   - `pageCount`: integer `1..2000`.
   - `fileSizeBytes`: integer `>= 0`.

## 2. The "Dirty Dozen" Payloads
1. **Unauthenticated Read**: `auth == null` reading `/users/alice_123/documents/doc_1` -> REJECT.
2. **Cross-User Read**: `auth.uid == "bob_456"` reading `/users/alice_123/documents/doc_1` -> REJECT.
3. **Cross-User List**: `auth.uid == "bob_456"` listing `/users/alice_123/documents` -> REJECT.
4. **Identity Spoofing on Create**: `auth.uid == "alice_123"` creating `/users/alice_123/documents/doc_1` with `userId: "bob_456"` -> REJECT.
5. **Shadow / Ghost Field Injection**: Creating or updating a document with an undeclared field `isAdmin: true` -> REJECT via `.keys().hasOnly(...)`.
6. **Immutable Field Mutation (`createdAt`)**: Updating a document with a modified `createdAt` timestamp -> REJECT.
7. **Immutable Field Mutation (`userId`)**: Updating a document to transfer ownership `userId: "bob_456"` -> REJECT.
8. **Type Poisoning (`pageCount`)**: Setting `pageCount: "five"` or `pageCount: 0` -> REJECT.
9. **String Overflow (`title`)**: Setting `title` to a 500-character string (`> 200`) -> REJECT.
10. **Unbounded Tag Array**: Setting `tags` to an array of 15 items (`> 10`) -> REJECT.
11. **Future Timestamp Spoofing**: Setting `createdAt` to a timestamp in the future (`> request.time`) -> REJECT.
12. **Folder Cross-User Write**: `auth.uid == "bob_456"` creating `/users/alice_123/folders/folder_1` -> REJECT.
