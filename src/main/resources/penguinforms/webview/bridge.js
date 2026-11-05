// Penguin Forms' stand-in for VS Code's webview API, so NetForms' designer.js runs unchanged inside Rider's
// built-in browser (JCEF). Messages to Rider go through a JBCefJSQuery; messages from Rider arrive as
// window 'message' events, exactly as in VS Code.
//
// Messages to Rider are sent ONE AT A TIME: each waits for Rider's "got it" before the next goes. Placing a control
// sends two messages in the same instant (the tool is back on the Pointer, then the add itself); in real Rider the
// second one could be lost when both went at once. Every message carries a number, so Rider can log a gap, and a
// message Rider didn't confirm is sent again once, then reported.
(function () {
	'use strict';
	var send = /*FW_SEND*/null;
	var state = /*FW_STATE*/null;
	var queue = [];
	var busy = false;
	var seq = 0;
	var problems = [];

	function pump() {
		if (busy || !queue.length) return;
		busy = true;
		var item = queue.shift();
		var finished = false;
		var timer = null;
		function next(ok, why) {
			if (finished) return;
			finished = true;
			clearTimeout(timer);
			if (!ok) {
				if (item.tries < 2) { item.tries++; queue.unshift(item); }
				else problems.push('message ' + item.seq + ' (' + item.type + ') was not confirmed by Rider: ' + why);
			}
			busy = false;
			if (problems.length && !queue.length) {
				var text = problems.join('; ');
				problems = [];
				enqueue({ type: '__fwLog', level: 'warn', message: 'Penguin Forms add: bridge problem: ' + text });
			}
			setTimeout(pump, ok ? 0 : 50);
		}
		// No answer within 4 s: send it again (once), then report it.
		timer = setTimeout(function () { next(false, 'no answer in 4 s'); }, 4000);
		try {
			send(item.text, function () { next(true); }, function (code, message) { next(false, 'error ' + code + ' ' + message); });
		} catch (e) {
			next(false, 'exception ' + e);
		}
	}

	function enqueue(message) {
		message.__seq = ++seq;
		queue.push({ seq: message.__seq, type: message.type, text: JSON.stringify(message), tries: 1 });
		pump();
	}

	window.__penguinFormsSend = enqueue;
	// Script errors are reported to Rider's log (Help → Show Log in Files), so problems can be found.
	window.addEventListener('error', function (e) {
		try { enqueue({ type: '__fwLog', level: 'error', message: 'Penguin Forms page error: ' + String(e.message) + ' at ' + e.filename + ':' + e.lineno }); } catch (_) {}
	});
	// Hooks NetForms' designer.js calls (patch 0002): Rider's native Toolbox and Properties follow them.
	window.__netformsSelectionChanged = function (ids) { enqueue({ type: '__fwSelection', ids: ids }); };
	window.__netformsToolChanged = function (tool) { enqueue({ type: '__fwTool', tool: tool }); };
	window.acquireVsCodeApi = function () {
		return {
			postMessage: function (message) { enqueue(message); },
			getState: function () { return state; },
			setState: function (s) { state = s; enqueue({ type: '__fwState', state: s }); },
		};
	};
	// The webview never goes online: no fetch, no XHR, no WebSocket (Penguin Forms makes no network requests).
	window.fetch = function () { return Promise.reject(new Error('Penguin Forms: no network')); };
	window.XMLHttpRequest = function () { throw new Error('Penguin Forms: no network'); };
	window.WebSocket = function () { throw new Error('Penguin Forms: no network'); };
})();
