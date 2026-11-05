// Penguin Forms additions to the NetForms webview (run after designer.js).
(function () {
	'use strict';
	window.addEventListener('message', function (e) {
		var m = e.data;
		if (m && m.type === 'fwFatal') {
			var stage = document.getElementById('stage');
			if (stage) {
				stage.innerHTML = '';
				var box = document.createElement('div');
				box.className = 'fw-fatal';
				box.textContent = m.message;
				stage.appendChild(box);
			}
		}
	});
})();
