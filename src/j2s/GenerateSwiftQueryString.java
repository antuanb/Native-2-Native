package j2s;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import com.google.code.stackexchange.schema.Answer;

public class GenerateSwiftQueryString {

	private static String title = "";
	private static String body = "";
	public static MetaData mdQuery = new MetaData();
	private static boolean initialQuery = true;
	private static ArrayList<String> globalKeywords = new ArrayList<String>();
	public static ArrayList<String> controlFlowCode = new ArrayList<String>();
	public static ArrayList<String> controlFlowCodeSwift = new ArrayList<String>();
	private static HashSet<String> javaAccessModifiers;
	private static HashSet<String> javaNonAccessModifiers;
	public static String inputLanguage = "";

	static {
		javaAccessModifiers = new HashSet<String>();
		javaAccessModifiers.add("public");
		javaAccessModifiers.add("private");
		javaAccessModifiers.add("protected");
	}

	static {
		javaNonAccessModifiers = new HashSet<String>();
		javaNonAccessModifiers.add("static");
		javaNonAccessModifiers.add("final");
		javaNonAccessModifiers.add("abstract");
		javaNonAccessModifiers.add("synchronized");
		javaNonAccessModifiers.add("volatile");
	}

	public static ArrayList<String> executeFrequencyAnalysis(String filepath) {
		body = "";
		globalKeywords = new ArrayList<String>();
		BufferedReader br = null;
		boolean flag = true;
		try {
			String sCurrentLine;
			br = new BufferedReader(new FileReader(filepath));
			while ((sCurrentLine = br.readLine()) != null) {
				sCurrentLine = sCurrentLine.trim();
				if (!sCurrentLine.equals("")) {
					if (flag) {
						if (sCurrentLine.startsWith("public") || sCurrentLine.startsWith("private")) {
							if (sCurrentLine.contains(" func ")) {
								inputLanguage = "swift";
							} else {
								inputLanguage = "java";
							}
							flag = false;
							title = sCurrentLine;
							parseControlFlowLine(sCurrentLine, true);
						} else if (sCurrentLine.startsWith("func")) {
							inputLanguage = "swift";
							flag = false;
							title = sCurrentLine;
							parseControlFlowLine(sCurrentLine, true);
						}
						// carry over method header comments to swift file
						else {
							controlFlowCode.add(sCurrentLine);
							StringTokenizer st = new StringTokenizer(sCurrentLine);
							while (st.hasMoreTokens()) {
								body += st.nextToken() + " ";
							}
						}
					} else {
						parseControlFlowLine(sCurrentLine, false);
						StringTokenizer st = new StringTokenizer(sCurrentLine);
						while (st.hasMoreTokens()) {
							body += st.nextToken() + " ";
						}
					}
					
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)
					br.close();
					controlFlowCode.add("INSERT RESULT");
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}

		generateQueryMetaDataObject();

		return globalKeywords;
	}

	public static void main(String[] args) throws IOException {
//		String line = "private static String[] parseControlFlowLine(ArrayList<String> line, ArrayList<Boolean> isHeader) {";
		// String line =
		// "public static ArrayList<Double> main(String[] args) {";
//		parseMethodHeader(line);
//		String methodHeader = generateSwiftMethodHeader(controlFlowCodeSwift);
//		System.out.println(methodHeader);
		
		String filepath = "/Users/sanchit/Downloads/methodSelection.txt";
//		String filepath = "C:\\Users\\Sanchit\\Downloads\\methodSelection.txt";
		String sCurrentLine = "";
		Boolean variableParse = false;
		BufferedReader br = new BufferedReader(new FileReader(filepath));
		while ((sCurrentLine = br.readLine()) != null) {
			sCurrentLine = sCurrentLine.trim();
			String varType = sCurrentLine.split(" ")[0];
			if (sCurrentLine.startsWith("public")) {
			parseMethodHeader(sCurrentLine);
			String methodHeader = generateSwiftMethodHeader(controlFlowCodeSwift);
			controlFlowCodeSwift.clear();
			System.out.println(methodHeader);
			} else if (sCurrentLine.startsWith("System.out.println")){
				String[] printString = sCurrentLine.split("\"");
				System.out.println("print(\"" + printString[1] + "\")");
				variableParse = true;
			} else if (variableParse) {
				variableParse = false;
				String type = processType(varType.toLowerCase());
				if (type != "null") {
					System.out.println("let " + sCurrentLine.split(" ")[1] + ":" + type + " = " + sCurrentLine.split(" ")[3].split(";")[0]);
				}
			} else {
				System.out.println(sCurrentLine);
			}
		}
		
//		executeFrequencyAnalysis(filepath);
//		for (String test : controlFlowCode) {
//			if (!test.equals("")) {
//				System.out.println(test);
//			}
//		}
	}

	public static void parseMethodHeader(String line) {
		String[] tokens = line.split("(\\()|(\\))|(\\ )|(, )|(<)|(>)");

		Boolean returnHandled = false;
		Boolean methodNameHandled = false;
		Boolean isParameterName = false;
		for (int i = 0; i < tokens.length; i++) {
			String token = tokens[i];
			if (token.equals("")) {
				continue;
			}
			if (!returnHandled) {
				if (javaAccessModifiers.contains(token)) {
					continue;
				} else if (javaNonAccessModifiers.contains(token)) {
					continue;
				} else {
					String objectType = processType(token.toLowerCase());
					if (objectType.equals("List")) {
						controlFlowCodeSwift.add("[" + processType(tokens[i + 1].toLowerCase()) + "]");
						i++;
						returnHandled = true;
						continue;
					} else if (objectType.equals("null")) {
						returnHandled = true;
						if (token.contains("[]")) {
							controlFlowCodeSwift.add("[" + processType(token.split("(\\[\\])")[0].toLowerCase()) + "]");
						} else {
							controlFlowCodeSwift.add("/*Unknown token: " + token + "*/");
						}
					} else {
						returnHandled = true;
						controlFlowCodeSwift.add(objectType);
					}
				}
			} else {
				if (!methodNameHandled) {
					controlFlowCodeSwift.add(token);
					methodNameHandled = true;
				} else {
					String objectType = processType(token.toLowerCase());
					if (!isParameterName) {
						if (objectType.equals("List")) {
							controlFlowCodeSwift.add("[" + processType(tokens[i + 1].toLowerCase()) + "]");
							i++;
						} else if (objectType.equals("null")) {
							if (token.contains("[]")) {
								controlFlowCodeSwift.add("[" + processType(token.split("(\\[\\])")[0].toLowerCase()) + "]");
							} else {
								if (token.contains("{")) {
									controlFlowCodeSwift.add(token);
								} else {
									controlFlowCodeSwift.add("/*Unknown token: " + token + "*/");
								}
							}
						} else {
							controlFlowCodeSwift.add(objectType);
						}
						isParameterName = true;
					} else {
						if (objectType.equals("null")) {
							if (token.contains("[]")) {
								controlFlowCodeSwift.add("[" + processType(token.split("(\\[\\])")[0].toLowerCase()) + "]");
							} else {
								controlFlowCodeSwift.add(token);
							}
						} else {
							controlFlowCodeSwift.add(objectType);
						}
						isParameterName = false;
					}
				}
			}
		}
	}

	private static String generateSwiftMethodHeader(ArrayList<String> controlFlowCodeSwift) {
		String result = "func ";
		ArrayList<String> parameterTypes = new ArrayList<String>();
		ArrayList<String> parameterNames = new ArrayList<String>();

		String returnType = controlFlowCodeSwift.get(0);
		String methodName = controlFlowCodeSwift.get(1);

		for (int i = 2; i < controlFlowCodeSwift.size(); i++) {
			String token = controlFlowCodeSwift.get(i);
			if (token.contains("{")) {
				break;
			}

			if (i % 2 == 0) {
				parameterTypes.add(token);
			} else {
				parameterNames.add(token);
			}
		}
		String parameterString = "";
		for (int i = 0; i < parameterTypes.size(); i++) {
			if (i + 1 == parameterTypes.size()) {
				parameterString += parameterNames.get(i) + ": " + parameterTypes.get(i);
			} else {
				parameterString += parameterNames.get(i) + ": " + parameterTypes.get(i) + ", ";
			}
		}

		if (returnType.equals("void")) {
			result += methodName + "(" + parameterString + ") {";
		} else {
			result += methodName + "(" + parameterString + ") -> " + returnType + " {";
		}
		return result;
	}

	public static String processType(String token) {
		switch (token) {
		case "void":
			return "void";
		case "string":
			return "String";
		case "double":
			return "Double";
		case "int":
			return "Int";
		case "integer":
			return "Int";
		case "char":
			return "Character";
		case "boolean":
			return "Bool";
		case "arraylist":
			return "List";
		case "list":
			return "List";
		case "map":
			return "[String, String]";
		default:
			return "null";
		}
	}

	private static void parseControlFlowLine(String line, boolean isHeader) {
		if (inputLanguage == "java") {
			if (isHeader) {
				parseMethodHeader(line);
				String methodHeader = generateSwiftMethodHeader(controlFlowCodeSwift);
				controlFlowCode.add(methodHeader);
				controlFlowCode.add("// Insert Native-2-Native Result");
			} else {
				String curLine = controlFlowParser(line);
				controlFlowCode.add(curLine);
			}
		} else if (inputLanguage == "swift") {
//			if (isHeader) {
//				parseMethodHeader(line);
//				String methodHeader = generateSwiftMethodHeader(controlFlowCodeSwift);
//				controlFlowCode.add(methodHeader);
//				controlFlowCode.add("// Insert Native-2-Native Result");
//			} else {
//				String curLine = controlFlowParser(line);
//				controlFlowCode.add(curLine);
//			}
		}
	}

	private static String controlFlowParser(String line) {
		if (line.startsWith("if")) {
			String conditionalLogic = line.substring(line.indexOf("("), line.indexOf(") ") + 1);
			return "if /*" + conditionalLogic + "*/ {\r\n" ;
		} else if (line.startsWith("for")) {
			String iterationLogic = line.substring(line.indexOf("(") + 1, line.indexOf(") "));
			String[] iterationTokens = iterationLogic.split(";");
			
			String initializationVariable = iterationTokens[0].split("=")[0].split(" ")[1];
			String initializationIndex = iterationTokens[0].split("=")[1].trim();
			String conditionLogicSymbol = iterationTokens[1].split(" ")[2];
			String conditionEnd = "/*" + iterationTokens[1].split(" ")[3] + "*/";
			String increment = iterationTokens[2].trim();
			
			String iterationLogicFinal = "for var " + initializationVariable + " = " + initializationIndex
					+ "; " + initializationVariable + " " + conditionLogicSymbol + " " + conditionEnd
					+ "; " + increment + " {";
			
			return iterationLogicFinal;
		} else if (line.startsWith("while")) {
			String conditionalLogic = line.substring(line.indexOf("("), line.indexOf(") ") + 1);
			return "while /*" + conditionalLogic + "*/ {";
		} else if (line.startsWith("} else {")) {
			return "} else {\r\n";
		} else if (line.startsWith("} else if")) {
			String conditionalLogic = line.substring(line.indexOf("("), line.indexOf(") ") + 1);
			return "} else if /*" + conditionalLogic + "*/ {\r\n";
		} else if (line.startsWith("return ")) {
			String returnValue = line.substring(7, line.length()-1);
			return "return /*" + returnValue + "*/";
		} else if (line.equals("}")) {
			return line;
		} else {
			return "";
		}
	}

	private static void generateQueryMetaDataObject() {
		String[] tokens = title.split("(?<=[a-z])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])|([0-9]+)|=|(\\()|(\\))|(\\.)|(\\_)|(\\n)|(\\,)");
		HashSet<String> titleTokens = new HashSet<String>();
		for (int i = 0; i < tokens.length; i++) {
			titleTokens.add(tokens[i].toLowerCase());
		}
		mdQuery.setTitleTokens(titleTokens);

		HashMap<String, Integer> tempFreq = new HashMap<String, Integer>();

		Answer a = new Answer();
		a.setBody(body);
		a.setTitle(title);
		AnswerWrapper aw = new AnswerWrapper(null, a, 0.0);

		tempFreq = SearchAndRank.createTokenFrequency(aw, false);
		SearchAndRank.DSet.add(tempFreq.keySet());
		mdQuery.setFrequency(tempFreq);
	}

	public static HashMap<String, Integer> removeSubstring(HashMap<String, Integer> frequency) {
		List<Map.Entry<String, Integer>> list = new LinkedList<>(frequency.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
			@Override
			public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
				if (o1.getKey().length() < o2.getKey().length()) {
					return -1;
				} else if (o1.getKey().length() == o2.getKey().length()) {
					return 0;
				} else {
					return 1;
				}
			}
		});
		Collections.reverse(list);
		Map<String, Integer> fullResult = new LinkedHashMap<>();
		for (Map.Entry<String, Integer> entry : list) {
			fullResult.put(entry.getKey(), entry.getValue());
		}

		List<String> keySetFrequency = new LinkedList<String>();
		keySetFrequency.addAll(fullResult.keySet());
		List<Integer> valueSetFrequency = new LinkedList<Integer>();
		valueSetFrequency.addAll(fullResult.values());
		HashMap<String, Integer> newFrequency = new HashMap<String, Integer>();
		ArrayList<String> removalSetKey = new ArrayList<String>();
		ArrayList<Integer> removalSetValue = new ArrayList<Integer>();
		for (int i = 0; i < keySetFrequency.size(); i++) {
			boolean flag = true;
			for (int j = 0; j < removalSetKey.size(); j++) {
				if (removalSetKey.get(j).contains(keySetFrequency.get(i)) && keySetFrequency.get(i).length() > 2) {
					removalSetValue.set(j, valueSetFrequency.get(i) + removalSetValue.get(j));
					flag = false;
					break;
				}
			}
			if (flag) {
				removalSetKey.add(keySetFrequency.get(i));
				removalSetValue.add(valueSetFrequency.get(i));
			}
		}
		for (int i = 0; i < removalSetKey.size(); i++) {
			newFrequency.put(removalSetKey.get(i), removalSetValue.get(i));
		}

		return newFrequency;
	}

	public static ArrayList<String> sortByValue(HashMap<String, Integer> frequency) {
		HashMap<String, Integer> newFrequency = removeSubstring(frequency);
		List<Map.Entry<String, Integer>> list = new LinkedList<>(newFrequency.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
			@Override
			public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
				return (o1.getValue()).compareTo(o2.getValue());
			}
		});

		Map<String, Integer> fullResult = new LinkedHashMap<>();
		ArrayList<String> result = new ArrayList<String>();
		for (Map.Entry<String, Integer> entry : list) {
			fullResult.put(entry.getKey(), entry.getValue());
			result.add(entry.getKey());
		}
		Collections.reverse(result);
		System.out.println(fullResult.toString());
//		if (initialQuery) {
//			initialQuery = false;
			globalKeywords.add(result.get(0));
			globalKeywords.add(result.get(1));
			globalKeywords.add(result.get(2));
//		}
		return result;
	}
}
