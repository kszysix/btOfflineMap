import Toybox.Communications;
import Toybox.Position;
import Toybox.Lang;
import Toybox.System;
import Toybox.WatchUi;

// Phase 1: HELLO / HELLO_ACK / GPS_UPDATE only. Wire format: shared/protocol/protocol.md.
// Everything here is watch -> phone plus the one phone -> watch reply (HELLO_ACK); no
// map/activity messages yet (§39 Phase 1 scope).
//
// `method(:symbol)` needs an object instance as `self` — it doesn't resolve from a bare
// module-level function — so the actual work lives in GarminLinkService (a class), and this
// module is just a thin singleton accessor for the View to read state from.
module GarminLink {
    var _service as GarminLinkService?;

    function start() as Void {
        if (_service == null) {
            _service = new GarminLinkService();
        }
        _service.start();
    }

    function getStatus() as String {
        return (_service != null) ? _service.status : "starting";
    }

    function getLastPosition() as Position.Info? {
        return (_service != null) ? _service.lastPosition : null;
    }
}

class GarminLinkService {
    const PROTOCOL_VERSION = 1;

    var status as String = "starting";
    var lastPosition as Position.Info?;

    var _gpsSeq as Number = 0;

    function initialize() {
    }

    function start() as Void {
        Communications.registerForPhoneAppMessages(method(:onPhoneMessage));
        sendHello();
        Position.enableLocationEvents(Position.LOCATION_CONTINUOUS, method(:onPosition));
    }

    function sendHello() as Void {
        status = "sending HELLO";
        var msg = { "v" => PROTOCOL_VERSION, "type" => "HELLO", "seq" => 0 };
        Communications.transmit(msg, null, new GarminLinkListener(self));
    }

    function onPhoneMessage(msg as Communications.PhoneAppMessage) as Void {
        var data = msg.data;
        if (data instanceof Dictionary && data.hasKey("type") && data["type"].equals("HELLO_ACK")) {
            status = "connected";
            WatchUi.requestUpdate();
        }
    }

    function onPosition(info as Position.Info) as Void {
        lastPosition = info;

        if (info.position != null) {
            var deg = info.position.toDegrees();
            var msg = {
                "v" => PROTOCOL_VERSION,
                "type" => "GPS_UPDATE",
                "seq" => _gpsSeq,
                "lat" => deg[0],
                "lon" => deg[1],
                "alt" => info.altitude,
                "ts" => System.getTimer(),
            };
            _gpsSeq += 1;
            Communications.transmit(msg, null, new GarminLinkListener(self));
        }

        WatchUi.requestUpdate();
    }

    // Fire-and-forget per §35 (activity/GPS delivery must never block on transport health) —
    // a failed transmit just gets retried on the next GPS fix, nothing here blocks onPosition.
    function onTransmitError() as Void {
        status = "transmit error";
        WatchUi.requestUpdate();
    }
}

class GarminLinkListener extends Communications.ConnectionListener {
    var _owner as GarminLinkService;

    function initialize(owner as GarminLinkService) {
        Communications.ConnectionListener.initialize();
        _owner = owner;
    }

    function onComplete() as Void {
    }

    function onError() as Void {
        _owner.onTransmitError();
    }
}
