# Analytics events

What the Android app tells Yandex AppMetrica about how it is used, and how to add to it. The reasoning is in
[ADR-0029](DECISIONS.md#adr-0029); the privacy page says the same to the user. Crash reports (Java, native and ANR)
go to the same place and are not events: AppMetrica takes them itself.

The list is `AnalyticsEvent` in `:core:domain`; a test checks every name and prop value is a short snake_case word,
so this table and that class are the same list: change one, change the other.

## Catalog

| Event | When | Props |
| --- | --- | --- |
| `screen_view` | a screen comes to the front, going forward or back | `screen`: `ObjectsList`, `ObjectDetails`, `ObjectEdit`, `PhotoViewer`, `Gallery`, `Settings`, `Finance`, `FinanceJournal`, `Materials`, `Journal`, `Map`, `PlacePicker`, `Tasks`, `Expenses`, `Trash`, `Other` |
| `object_created` | an object was made (on the phone, whether or not the server has it yet) | `status`: `planned`, `in_progress`, `done`, `paused` |
| `object_status_changed` | the status of an object was changed from its card | `to`: the same four |
| `object_deleted` | an object was moved to the trash | none |
| `photo_added` | a picture was uploaded to an object | `kind`: `photo`, `receipt` |
| `receipt_paid_changed` | a receipt was marked paid or not paid, from the expenses list or the photo viewer | `paid`: `yes`, `no` |
| `images_shared` | the share sheet was opened with receipt pictures from the expenses list | `count`: `one`, `few` (2 to 4), `many` (5 or more) |
| `payment_added` | a payment was added to the finance of an object | none |
| `extra_work_added` | an extra work was added to the finance of an object | none |
| `task_created` | a task was added to a plan | `reminder`: `yes` if it has a time, `no` if not |

An event is reported when the write went through, never for a refused or failed one. Editing, deleting a payment and
the like are not reported. Sessions and their length come from AppMetrica's own activity tracking, not from an event.

## What is never in an event

No text the user typed (titles, notes, names, phones, addresses, search), no amount, no id of an object, photo or
task, no account, no token, no server address. A prop is a word from a fixed set or `yes`/`no`; the types allow
nothing else, because an event can only be made from the sealed `AnalyticsEvent`. `screen_view` names the screen by
the `AppScreen` enum, never by a navigation key, whose arguments are ids.

## How it reaches AppMetrica

1. A screen's navigation or a use case calls `AnalyticsTracker.track(event)`; features never import the SDK.
2. `:app` binds the tracker to `AppMetricaTracker`, which calls `AppMetrica.reportEvent`. Without an API key in the
   build, or in the browser client, the tracker is a no-op (`AnalyticsTracker.None`).
3. AppMetrica activates in `ProrabApplication.onCreate` with sending switched off; the settings choice is read and
   sending is switched on only if the user has not opted out. The switch in the settings flips both the SDK and the
   tracker, so events are dropped, not queued, while it is off.

Debug builds never get a key (so a build from the IDE cannot reach the store app's data); a release build gets it
from `prorab.<flavor>.appmetricaKey` in `local.properties` (README, "Build and install the app").

## Adding an event

1. Name the question the event answers before the event; an unused event is data nobody asked for.
2. Add a case to `AnalyticsEvent`; props are an enum or yes/no, never a string from the user. Add it to
   `AnalyticsEventTest`.
3. Report it from the use case that does the thing, through `Result.tracked`, so it is not repeated by each screen
   and a failure is not counted; or from the navigation for a screen.
4. Add a row here and, if it is a new kind of fact, a line on the privacy page.
5. Test that it is reported on success and not on failure (`AnalyticsTrackingTest`).
