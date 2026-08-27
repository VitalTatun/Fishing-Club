# Implementation Plan - Update Profile Settings Screen

Update the Profile Settings screen to match the Figma design, changing the layout from a simple form to a settings-list style.

## Proposed Changes

### [app] Component: UI Screens

#### [MODIFY] [EditProfileScreen.kt](file:///Users/vital/AndroidStudioProjects/Fishing/app/src/main/java/com/example/fishing/ui/screens/profile/EditProfileScreen.kt)
- Update the layout to match the Figma design.
- Change the header title to "Настройки профиля".
- Implement a list-style UI for profile information:
    - User photo section with a 140dp circular avatar and a tonal camera icon button.
    - Info section with "Name" (clickable with chevron) and "Email" (non-clickable).
    - Actions section with "Change password" and "Delete account" (both clickable with chevrons).
- Add `email` parameter to the `EditProfileScreen` composable.
- Add `onChangePasswordClick` and `onDeleteAccountClick` callbacks.
- Since the design doesn't show a "Save" button in the top bar, I will remove it and assume that editing the name (which will likely happen in a dialog) will trigger the save, or I'll keep the top-bar save button for now if it's still needed for name changes. *Update: I'll keep the Save button but make it consistent with the "Check" icon if the user decides to edit the name.* Actually, if I follow the design strictly, there is no save button in the top bar. I will remove it and add a way to edit the name (e.g. via a dialog when the name item is clicked).

### [app] Component: Navigation

#### [MODIFY] [FishingNavHost.kt](file:///Users/vital/AndroidStudioProjects/Fishing/app/src/main/java/com/example/fishing/ui/navigation/FishingNavHost.kt)
- Pass `email` to `EditProfileScreen`.
- Add placeholder callbacks for `onChangePasswordClick` and `onDeleteAccountClick`.

## Verification Plan

### Automated Tests
- Build the project to ensure no compilation errors.
- Render Compose Preview for `EditProfileScreen` to verify the UI.

### Manual Verification
- Deploy to a device/emulator.
- Navigate to the Profile Settings screen.
- Verify the layout matches the Figma design:
    - Avatar size and camera icon position.
    - Name and Email display.
    - Change password and Delete account items.
