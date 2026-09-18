import Toybox.Graphics;
import Toybox.Lang;
import Toybox.WatchUi;

// Phase 1: shows link status and the last GPS fix. Replaced by the real map view starting
// Phase 4 (docs/PROJECT_PLAN.md §39) — this is deliberately plain text, no styling effort.
class FR255MapAppView extends WatchUi.View {

    function initialize() {
        View.initialize();
    }

    function onUpdate(dc as Graphics.Dc) as Void {
        dc.setColor(Graphics.COLOR_WHITE, Graphics.COLOR_BLACK);
        dc.clear();

        var lines = [GarminLink.getStatus()] as Array<String>;

        var pos = GarminLink.getLastPosition();
        if (pos != null && pos.position != null) {
            var deg = pos.position.toDegrees();
            lines.add(deg[0].format("%.5f"));
            lines.add(deg[1].format("%.5f"));
        }

        var lineHeight = 20;
        var y = (dc.getHeight() / 2) - ((lines.size() - 1) * lineHeight / 2);
        for (var i = 0; i < lines.size(); i += 1) {
            dc.drawText(
                dc.getWidth() / 2,
                y,
                Graphics.FONT_SMALL,
                lines[i],
                Graphics.TEXT_JUSTIFY_CENTER | Graphics.TEXT_JUSTIFY_VCENTER
            );
            y += lineHeight;
        }
    }
}
