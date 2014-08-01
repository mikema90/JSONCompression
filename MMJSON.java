package com.ebay.cb.cjson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * 
 * @author MIKE MA
 * 
 */
public class MMJSON {
	// for compress
	public static Map<String, Integer> meta = new HashMap<String, Integer>();
	public static Map<ArrayList<Integer>, Integer> l2 = new HashMap<ArrayList<Integer>, Integer>();
	public static int metaIndex = -1;
	public static int l2Index = -1;

	// for expand
	public static JSONArray jmeta = new JSONArray();
	public static JSONArray jl2 = new JSONArray();

	public MMJSON() {
		// TODO Auto-generated constructor stub
	}

	public static JSONObject Compress(JSONObject jo) {
		int original_len = jo.toString().length();
		// initialize
		meta.clear();
		l2.clear();
		metaIndex = -1;
		l2Index = -1;
		// ---------------
		JSONObject cjo = new JSONObject();
		long startTime = System.currentTimeMillis();
		JSONObject v = (JSONObject) compress_process(jo);
		long estimatedTime = System.currentTimeMillis() - startTime;
		System.out.println("Compress estimatedTime is " + estimatedTime + "ms");
		cjo.put("format", "cjson");
		fillMeta(cjo);
		fillL2(cjo);
		cjo.put("v", v);

		int compressed_len = cjo.toString().length();
		double crate = ((double) compressed_len / (double) original_len);
		crate = Math.round(crate * 100.0) / 100.0;
		System.out.println("rate of compressed is " + crate);
		return cjo;
	}

	@SuppressWarnings("rawtypes")
	public static Object compress_process(Object root) {
		if (root instanceof JSONArray) {
			JSONArray ja_root = (JSONArray) root;
			// process each item in the array.
			JSONArray v = new JSONArray();
			for (int i = 0; i < ja_root.size(); i++) {
				v.add(compress_process(ja_root.get(i)));
			}
			return v;
		} else if (root instanceof JSONObject) {
			JSONObject jo_root = (JSONObject) root;
			JSONObject v = new JSONObject();
			// for l2
			ArrayList<Integer> metalist = new ArrayList<Integer>();
			int index = -1;
			// for v
			JSONArray valuelist = new JSONArray();
			// For each key
			Iterator iter = jo_root.keys();
			while (iter.hasNext()) {
				String key = (String) iter.next();
				if (meta.containsKey(key)) {
					metalist.add(meta.get(key));
				} else {
					metaIndex++;
					meta.put(key, metaIndex);
					metalist.add(metaIndex);
				}
				valuelist.add(compress_process(jo_root.get(key)));
			}
			// check l2
			if (l2.containsKey(metalist)) {
				index = l2.get(metalist);
			} else {
				l2Index++;
				l2.put(metalist, l2Index);
				index = l2Index;
			}
			valuelist.add(0, index);
			v.put("", valuelist);
			return v;
		} else {
			Object v = root;
			return v;
		}
	}

	public static void fillMeta(JSONObject cjo) {
		int metaCount = meta.size();
		ArrayList<String> metas = new ArrayList<String>(metaCount);
		// fill the capacity of array
		for (int i = 0; i < metaCount; i++) {
			metas.add(i, "");
		}
		for (Map.Entry<String, Integer> entry : meta.entrySet()) {
			metas.set(entry.getValue(), entry.getKey());
		}
		cjo.put("meta", metas);
	}

	public static void fillL2(JSONObject cjo) {
		int l2Count = l2.size();
		ArrayList<ArrayList<Integer>> l2s = new ArrayList<ArrayList<Integer>>(
				l2Count);
		// fill the capacity of array
		for (int i = 0; i < l2Count; i++) {
			l2s.add(i, new ArrayList<Integer>());
		}
		for (Map.Entry<ArrayList<Integer>, Integer> entry : l2.entrySet()) {
			l2s.set(entry.getValue(), entry.getKey());
		}
		cjo.put("l2", l2s);
	}

	public static JSONObject Expand(JSONObject cjo) {
		if (!"cjson".equals(cjo.get("format"))) {
			// not in cjson format. Return as is.
			return cjo;
		}

		jmeta = (JSONArray) cjo.get("meta");
		jl2 = (JSONArray) cjo.get("l2");
		return (JSONObject) expand_process(cjo.get("v"));
	}

	public static Object expand_process(Object root) {

		// if it's an array, then expand each element of the array.
		if (root instanceof JSONArray) {
			JSONArray ja_root = (JSONArray) root;
			JSONArray v = new JSONArray();
			for (int i = 0; i < ja_root.size(); i++) {
				v.add(expand_process(ja_root.get(i)));
			}
			return v;
		} else if (root instanceof JSONObject) {
			// if it's an object, then recreate the keys from the template
			// and expand.
			JSONObject jo_root = (JSONObject) root;
			JSONObject v = new JSONObject();
			JSONArray keys = getKeys((Integer) ((JSONArray) jo_root.get(""))
					.get(0));
			for (int i = 0; i < keys.size(); i++) {
				v.put(keys.get(i),
						expand_process(((JSONArray) jo_root.get("")).get(i + 1)));
			}
			return v;
		} else {
			Object v = root;
			return v;
		}
	}

	/**
	 * return keys according to l2Index
	 * 
	 * @param l2Index
	 * @returns {Array}
	 */
	public static JSONArray getKeys(int l2Index) {
		JSONArray l2_list = (JSONArray) jl2.get(l2Index);
		JSONArray keys = new JSONArray();
		for (int i = 0; i < l2_list.size(); i++) {
			int metaIndex = l2_list.getInt(i);
			keys.add(jmeta.get(metaIndex));
		}
		return keys;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println("experiment1......");
		JSONObject jo = new JSONObject();
		jo.put("data", 12);

		System.out.println("compress......");
		JSONObject cjo = Compress(jo);
		System.out.println("cjo:" + cjo);
		System.out.println("expand......");
		jo = Expand(cjo);
		System.out.println("jo:" + jo);
		// -----------------------------------
		System.out.println("experiment2......");
		jo.clear();
		JSONObject j1 = new JSONObject();
		JSONObject j2 = new JSONObject();
		JSONObject j3 = new JSONObject();
		JSONArray ja = new JSONArray();
		j1.put("x", 10);
		j1.put("y", 20);
		j2.put("x", 30);
		j2.put("y", 40);
		j3.put("x", 50);
		j3.put("y", 60);
		ja.add(j1);
		ja.add(j2);
		ja.add(j3);
		jo.put("data", ja);

		System.out.println("compress......");
		cjo = Compress(jo);
		System.out.println("cjo:" + cjo);
		System.out.println("expand......");
		jo = Expand(cjo);
		System.out.println("jo:" + jo);

		// -----------------------------------
		System.out.println("experiment from js......");
		cjo = JSONObject
				.fromObject("{\"format\":\"cjson\",\"meta\":[\"data\",\"name\",\"age\"],\"l2\":[[\"1\",\"2\"],[\"0\"]],\"v\":{\"\":[1,{\"\":[0,\"mike\",16]}]}}");
		System.out.println("expand......");
		jo = Expand(cjo);
		System.out.println("jo:" + jo);
	}

}
