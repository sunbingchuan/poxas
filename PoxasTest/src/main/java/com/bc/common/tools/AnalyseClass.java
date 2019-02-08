package com.bc.common.tools;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;

@SuppressWarnings("rawtypes")
public class AnalyseClass {

	private static Logger logger = LoggerFactory.getLogger(AnalyseClass.class);



	/**
	 * @param jarPath
	 * @return
	 * @throws IOException
	 */
	@SuppressWarnings({"resource"})
	public static ClassDescriptions analyseJarToDesc(String jarPath) throws IOException {
		List<List<Class>> classMap = new ArrayList<List<Class>>();
		Map<Class, Integer> timesMap = new HashMap<Class, Integer>();
		Map<Class, ClassEntity> entityMap = new HashMap<Class, ClassEntity>();
		URLClassLoader classLoader = new URLClassLoader(new URL[] {new URL("file:" + jarPath)},
				Thread.currentThread().getContextClassLoader());
		JarFile jar = new JarFile(jarPath);
		Enumeration<JarEntry> e = jar.entries();
		while (e.hasMoreElements()) {
			try {
				JarEntry entry = e.nextElement();
				if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
					String classname = entry.getName().replace(".class", "").replaceAll("/", ".");
					if (!classname.matches("[\\S]*\\$[\\d]+")) {
						analyse(classLoader, classname, classMap, timesMap, entityMap);
					}
				}
			} catch (Throwable e1) {
				logger.error("", e1);
			}
		}
		classMap = distinct(classMap);
		sort(classMap);
		logger.debug("analysed class map : {}", classMap);
		logger.debug("caculated class map : {}", timesMap);
		return new ClassDescriptions(classMap, timesMap, entityMap);
	}


	/**
	 * just split
	 * 
	 * @param classLoader
	 * @param classname
	 * @param classMap
	 * @param caculateMap
	 * @throws ClassNotFoundException
	 */
	private static void analyse(URLClassLoader classLoader, String classname, List<List<Class>> classMap,
			Map<Class, Integer> caculateMap, Map<Class, ClassEntity> entityMap) throws ClassNotFoundException {
		int times = 0;
		Class<?> cs1 = classLoader.loadClass(classname);
		for (List<Class> css : classMap) {
			Class<?> cs2 = css.get(0);
			if (cs1.equals(cs2)) {
				times++;
				break;
			}
			if (cs1.isAssignableFrom(cs2)) {
				css.add(css.set(0, cs1));
				times++;
			} else if (cs2.isAssignableFrom(cs1)) {
				css.add(cs1);
				times++;
			}
		}
		if (times == 0) {
			List<Class> css = new ArrayList<Class>();
			css.add(cs1);
			classMap.add(css);
			times++;
		}
		caculateMap.put(cs1, times);
		entityMap.put(cs1, new ClassEntity(cs1));
	}

	/**
	 * 
	 * merge repeating records
	 * 
	 * @param classMap
	 * @return
	 */
	private static List<List<Class>> distinct(List<List<Class>> classMap) {
		List<List<Class>> newClassMap = new ArrayList<List<Class>>();
		for (List<Class> ls : classMap) {
			boolean flag1 = false;
			for (List<Class> newls : newClassMap) {
				if (ls.get(0).equals(newls.get(0))) {
					// merge
					for (Class cs : ls) {
						boolean flag2 = false;
						for (Class cs1 : newls) {
							if (cs.equals(cs1)) {
								flag2 = true;
								break;
							}
						}
						if (!flag2) {
							newls.add(cs);
						}
					}
					flag1 = true;
					break;
				}
			}
			if (!flag1) {
				newClassMap.add(ls);
			}
		}
		return newClassMap;
	}

	/**
	 * 
	 * sorted by size desc
	 * 
	 * @param classMap
	 * @return
	 */
	private static void sort(List<List<Class>> classMap) {
		for (int i = 0; i < classMap.size(); i++) {
			for (int j = i + 1; j < classMap.size(); j++) {
				if (classMap.get(i).size() < classMap.get(j).size()) {
					classMap.set(j, classMap.set(i, classMap.get(j)));
				}
			}
		}
	}

	/**
	 * 
	 * @param c
	 * @return
	 */
	public static Set<Class> getReference(Class<?> c) {

		Set<Class> ref = new HashSet<Class>();
		Set<Annotation> ast = new HashSet<Annotation>();

		Field[] fs = c.getDeclaredFields();
		for (Field field : fs) {
			Annotation[] as = field.getDeclaredAnnotations();
			ast.addAll(Arrays.asList(as));
			ref.add(ref.getClass());
		}

		Method[] ms = c.getDeclaredMethods();
		for (Method method : ms) {
			Annotation[] as = method.getDeclaredAnnotations();
			ast.addAll(Arrays.asList(as));
			ref.addAll(Arrays.asList(method.getParameterTypes()));
			ref.add(method.getReturnType());
		}

		Constructor[] cs = c.getDeclaredConstructors();
		for (Constructor<?> constructor : cs) {
			Annotation[] as = constructor.getDeclaredAnnotations();
			ast.addAll(Arrays.asList(as));
			ref.addAll(Arrays.asList(constructor.getParameterTypes()));
		}

		for (Annotation a : ast) {
			if (!ref.contains(a.getClass())) {
				ref.add(a.getClass());
			}
		}

		return ref;

	}

	public static class ClassEntity {
		public static final String[] FILTER = new String[] {"java.lang"};

		private Set<Class> references = new HashSet<Class>();
		private Class<?> cs;
		private Map<String, Object> describeMap = new HashMap<>();

		public ClassEntity(Class<?> cs) {
			this.cs = cs;
			this.references = getReference(cs);
			this.describeMap = toDescribeMap();
		}

		public Map<String, Object> toDescribeMap() {
			List<String> refstrs = new ArrayList<>();
			List<String> mstrs = new ArrayList<>();
			List<String> fstrs = new ArrayList<>();
			for (Class<?> ref : references) {
				if (!filter(ref.getName(), FILTER)) {
					refstrs.add(ref.getName());
				}
			}

			Field[] fs = cs.getDeclaredFields();
			for (Field field : fs) {
				fstrs.add(field.getName());
			}
			Method[] ms = cs.getDeclaredMethods();
			for (Method method : ms) {
				mstrs.add(method.getName());
			}
			describeMap.put("references", refstrs);
			describeMap.put("fields", fstrs);
			describeMap.put("methods", mstrs);
			return describeMap;
		}

		public String toJSONString() {
			return JSON.toJSONString(describeMap);
		}

		public boolean filter(String name, String[] filters) {
			for (String filter : filters) {
				if (name.contains(filter)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public String toString() {
			return toJSONString();
		}

		public Set<Class> getReferences() {
			return references;
		}

		public void setReferences(Set<Class> references) {
			this.references = references;
		}

		public Class<?> getCs() {
			return cs;
		}

		public void setCs(Class<?> cs) {
			this.cs = cs;
		}

		public Map<String, Object> getDescribeMap() {
			return describeMap;
		}

		public void setDescribeMap(Map<String, Object> describeMap) {
			this.describeMap = describeMap;
		}

	}

	public static class ClassDescriptions {
		private List<List<Class>> classMap;
		private Map<Class, Integer> timesMap;
		private Map<Class, ClassEntity> entityMap;

		public ClassDescriptions(List<List<Class>> classMap, Map<Class, Integer> caculateMap,
				Map<Class, ClassEntity> entityMap) {
			this.timesMap = caculateMap;
			this.classMap = classMap;
			this.entityMap = entityMap;
		}

		public String toJSONString() {
			return JSON.toJSONString(this);
		}

		@Override
		public String toString() {
			return toJSONString();
		}

		public List<List<Class>> getClassMap() {
			return classMap;
		}

		public void setClassMap(List<List<Class>> classMap) {
			this.classMap = classMap;
		}

		public Map<Class, Integer> getCaculateMap() {
			return timesMap;
		}

		public void setCaculateMap(Map<Class, Integer> caculateMap) {
			this.timesMap = caculateMap;
		}

		public Map<Class, ClassEntity> getEntityMap() {
			return entityMap;
		}

		public void setEntityMap(Map<Class, ClassEntity> entityMap) {
			this.entityMap = entityMap;
		}

	}

	public static void main(String[] args) throws IOException {
		ClassDescriptions desc = analyseJarToDesc(
				"D:/JDP/Maven/repository/org/springframework/spring-context/4.1.7.RELEASE/spring-context-4.1.7.RELEASE.jar");
		System.out.println(desc);
	}
}
