# Redesign audit and verification

Baseline: e9600d664ff295bfb06c90cf64901274f927123b (previously verified source).
The app had one dashboard, native monitoring footer, menus, setup/update dialogs,
history selector and a dark Chronometer overlay. Core counting/storage/security
logic is preserved. Inventory and visual evidence are in ../../DESIGN.md.

Changes: true-data composition ring, separated readable hourly section, flat app
ranking, cohesive light indigo/blue Material palette, dialog/control shapes and
font hierarchy; dark widget styling, 48dp overlay height and disabled-animation
support. Selected date now survives Activity recreation. No new runtime dependency.

Acceptance: all original functional/device checks must pass. Added device check
writes synthetic history for 1999-01-01, selects it through the real date menu,
checks selection after rotation, and captures portrait/landscape/2x-font UI. This
is a dedicated test-device fixture, never seeded into production installations.

Local software emulator is not used: this environment has no /dev/kvm. Behavioral
and visual verification use accelerated CI emulators on API 30 and 36. Source
build, unit and lint verification also run locally. Exact current results are in
the redesign PR checks and final handoff, not inferred from previous APKs.

Native verification exposed QA problems rather than lost selected history: the
landscape failure capture still displayed the correct 1999 total, while the legacy
UiScrollable search stopped before the date control. The test now uses bounded
real swipes, current list bounds, and verifies the selected date plus loaded total
at the summary. Rotation uses actual display geometry, since a background app's
Application resources can retain the old orientation while Settings is focused.
API 30 shell could not pull scoped external storage; test captures now use private
files/qa and debug run-as tar extraction. No production storage permission is added.
Added app-list and 2x setup-dialog captures; setup and long update notes scroll while
the native dialog action buttons remain accessible.
