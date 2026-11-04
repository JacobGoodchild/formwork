// Formwork's stand-in for VS Code's webview API, so NetForms' designer.js runs unchanged inside Rider's
// built-in browser (JCEF). Messages to Rider go through a JBCefJSQuery; messages from Rider arrive as
// window 'message' events, exactly as in VS Code.
(function () {
	'use strict';
	var send = /*FW_SEND*/null;
	var state = /*FW_STATE*/null;
	window.__formworkSend = function (message) { send(JSON.stringify(message)); };
	window.acquireVsCodeApi = function () {
		return {
			postMessage: function (message) { send(JSON.stringify(message)); },
			getState: function () { return state; },
			setState: function (s) { state = s; send(JSON.stringify({ type: '__fwState', state: s })); },
		};
	};
	// The webview never goes online: no fetch, no XHR, no WebSocket (Formwork makes no network requests).
	window.fetch = function () { return Promise.reject(new Error('Formwork: no network')); };
	window.XMLHttpRequest = function () { throw new Error('Formwork: no network'); };
	window.WebSocket = function () { throw new Error('Formwork: no network'); };
})();
