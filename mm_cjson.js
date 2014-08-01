/**
 * @author MIKE MA
 */
var meta = null;
var l2 = null;
var metaIndex = -1;
var l2Index = -1;
(function() {
	/*
	 * CJSON.Compress() to compress json object CJSON.Expand() to expand json
	 * object.
	 */
	function Compress(jo) {
		var original_len = JSON.stringify(jo).length;
		// initialize
		meta = {};
		l2 = {};
		metaIndex = -1;
		l2Index = -1;
		// ---------------
		var cjo = {};
		var startTime = new Date().getTime();
		var v = compress_process(jo);
		var estimatedTime = new Date().getTime() - startTime;
		console.log("Compress estimatedTime is " + estimatedTime + "ms");
		cjo["format"] = "cjson";
		fillMeta(cjo);
		fillL2(cjo);
		cjo["v"] = v;

		var compressed_len = JSON.stringify(cjo).length;
		var crate = (compressed_len / original_len).toFixed(2);
		console.log("rate of compressed is " + crate);
		return cjo;
	}

	function compress_process(root) {
		var v = null;
		if (typeof root === 'object') {
			if (Object.prototype.toString.apply(root) === '[object Array]') {
				// process each item in the array.
				v = [];
				for (var i = 0; i < root.length; i++) {
					v.push(compress_process(root[i]));
				}
				return v;
			} else {
				v = {};
				// for l2
				var metalist = [];
				var index = -1;
				// for v
				var valuelist = [];
				// For each key
				var key = null;
				for (key in root) {
					if (key in meta) {
						metalist.push(meta[key]);
					} else {
						metaIndex++;
						meta[key] = metaIndex;
						metalist.push(metaIndex);
					}
					valuelist.push(compress_process(root[key]));
				}

				// check l2
				if (metalist in l2) {
					index = l2[metalist];
				} else {
					l2Index++;
					l2[metalist] = l2Index;
					index = l2Index;
				}
				valuelist.unshift(index);
				v[""] = valuelist;
				return v;
			}
		} else {
			v = root;
		}
		return v;
	}

	function fillMeta(cjo) {
		var metas = [];
		var key = null;
		for (key in meta) {
			var index = meta[key];
			metas[index] = key;
		}
		cjo["meta"] = metas;
	}

	function fillL2(cjo) {
		var l2s = [];
		var key = null;
		for (key in l2) {
			var index = l2[key];
			// convert key to array
			var keys = key.split(",");
			l2s[index] = keys;
		}
		cjo["l2"] = l2s;
	}

	function Expand(cjo) {
		if (typeof cjo !== "object" || !("format" in cjo)
				|| cjo["format"] !== "cjson") {
			// not in cjson format. Return as is.
			return cjo;
		}

		meta = cjo.meta;
		l2 = cjo.l2;
		return expand_process(cjo["v"]);
	}

	function expand_process(root) {
		var v;
		if (typeof root === 'object') {
			// if it's an array, then expand each element of the array.
			if (Object.prototype.toString.apply(root) === '[object Array]') {
				v = [];
				for (var i = 0; i < root.length; i++) {
					v.push(expand_process(root[i]));
				}
				;
			} else {
				// if it's an object, then recreate the keys from the template
				// and expand.
				v = {};
				var keys = getKeys(root[""][0]);
				for (var i = 0; i < keys.length; i++) {
					v[keys[i]] = expand_process(root[""][i + 1]);
				}
				;
			}
			;
		} else {
			v = root;
		}
		return v;
	}

	/**
	 * return keys according to l2Index
	 * 
	 * @param l2Index
	 * @returns {Array}
	 */
	function getKeys(l2Index) {
		var l2_list = l2[l2Index];
		var keys = [];
		for (var i = 0; i < l2_list.length; i++) {
			var metaIndex = l2_list[i];
			keys.push(meta[metaIndex]);
		}
		return keys;
	}
	;

	window.mm_cjson = {};
	window.mm_cjson.Compress = Compress;
	window.mm_cjson.Expand = Expand;

})();