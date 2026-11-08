// Penguin Forms additions to the NetForms webview (run after designer.js).
(function () {
	'use strict';
	// Visual Studio has no centre snaplines: NetForms' are switched off (not just hidden, or an unseen one could win).
	if (window.NetFormsGeometry) window.NetFormsGeometry.centerLines = false;
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
