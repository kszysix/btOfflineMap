import Toybox.Application;
import Toybox.Lang;
import Toybox.WatchUi;

// Phase 1: starts the HELLO/GPS_UPDATE link (GarminLink.mc) on app launch. No map/activity
// logic yet (docs/PROJECT_PLAN.md §39 Phase 1 scope).
class FR255MapAppApp extends Application.AppBase {

    function initialize() {
        AppBase.initialize();
    }

    function onStart(state as Dictionary?) as Void {
        GarminLink.start();
    }

    function getInitialView() as [Views] or [Views, InputDelegates] {
        return [ new FR255MapAppView() ];
    }
}
