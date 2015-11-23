package nlp.assignments;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Collections;
import java.util.Comparator;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import nlp.classify.*;
import nlp.util.CommandLineUtils;
import nlp.util.Counter;
import nlp.util.BoundedList;
import nlp.util.Pair;

/**
 * This is the main harness for assignment 2. To run this harness, use
 * <p/>
 * java nlp.assignments.ProperNameTester -path ASSIGNMENT_DATA_PATH -model
 * MODEL_DESCRIPTOR_STRING
 * <p/>
 * First verify that the data can be read on your system using the baseline
 * model. Second, find the point in the main method (near the bottom) where a
 * MostFrequentLabelClassifier is constructed. You will be writing new
 * implementations of the ProbabilisticClassifer interface and constructing them
 * there.
 */

public class ProperNameTester {
	
	public static class POSFeatureExtractor implements
			FeatureExtractor<String, String> {
		
		public Counter<String> extractFeatures(String name) {
			Counter<String> features = new Counter<String>();
			List<Character> charList = stringToList(name);
			BoundedList<Character> flexCharList = new BoundedList<Character>(charList);
			
			// add character unigram features
			//for (int i = 0; i < charList.size(); i++) {
			//	Character character = charList.get(i);
			//	Character prev = flexCharList.get(i-1);
			//	Character prevprev = flexCharList.get(i-2);
				//features.incrementCount("UNI-" + character, 1.0);
							
			//	if (prev != null) {
				//	features.incrementCount("BI-" + prev + character, 1.0);
			//		if (prevprev != null)
			//			features.incrementCount("TRI-" + prevprev + prev + character, 1.0);
			//	}
			//}
			int n = flexCharList.size();
			features.incrementCount("SUFFIX-" + flexCharList.get(n-3) + flexCharList.get(n-2) 
					+ flexCharList.get(n-1), 1.0);
			features.incrementCount("PREFIX-" + flexCharList.get(0) + flexCharList.get(1) + flexCharList.get(2), 1.0);
			if (Character.isUpperCase(flexCharList.get(0)))
				features.incrementCount("BeginsUppercase", 1.0);
			if (flexCharList.get(0).equals("h") && flexCharList.get(1).equals("t") && flexCharList.get(2).equals("t") &&
					flexCharList.get(3).equals("p"))
				features.incrementCount("BeginsHTTP", 1.0);
			if (Character.isDigit(flexCharList.get(0)))
				features.incrementCount("BeginsDigit", 1.0);
			else if (!Character.isLetterOrDigit(flexCharList.get(0)))
				features.incrementCount("BeginsPunctuation", 1.0);
			
			return features;	
		}
	}

	public static class ProperNameFeatureExtractor implements
			FeatureExtractor<String, String> {

		/**
		 * This method takes the list of characters representing the proper name
		 * description, and produces a list of features which represent that
		 * description. The basic implementation is that only character-unigram
		 * features are extracted. An easy extension would be to also throw
		 * character bigrams into the feature list, but better features are also
		 * possible.
		 */
		public Counter<String> extractFeatures(String name) {
			Counter<String> features = new Counter<String>();
			List<Character> charList = stringToList(name);
			BoundedList<Character> flexCharList = new BoundedList<Character>(charList);
			
			// add character unigram features
			for (int i = 0; i < charList.size(); i++) {
				Character character = charList.get(i);
				Character prev = flexCharList.get(i-1);
				Character prevprev = flexCharList.get(i-2);
				features.incrementCount("UNI-" + character, 1.0);
				if (Character.isUpperCase(character))
					features.incrementCount("UNI-" +
					Character.toLowerCase(character), 1.0);
				if (Character.isDigit(character))
					features.incrementCount("DIGITS", 1.0);
				
				if (!Character.isLetterOrDigit(character)) {
					if (Character.isWhitespace(character))
						features.incrementCount("HAS_WHITESPACE", 1.0);
					else
						features.incrementCount("HAS_PUNCTUATION", 1.0);
				}
				
				if (prev != null) {
					features.incrementCount("BI-" + prev + character, 1.0);
					if (prevprev != null)
						features.incrementCount("TRI-" + prevprev + prev + character, 1.0);
				}
			}
			
			if (name.length()<=5)
				features.incrementCount("NUM_CHAR_LESS_5", 1.0);
			else if (name.length()<=10)
				features.incrementCount("NUM_CHAR_LESS_10", 1.0);
			else if (name.length()<=15)
				features.incrementCount("NUM_CHAR_LESS_15", 1.0);
			else if (name.length()>25)
				features.incrementCount("NUM_CHAR_GREATER_25", 1.0);
				
			String[] words = name.split("\\W+");
			if (words.length==1)
				features.incrementCount("NUM_WORDS_1", 1.0);
			else if (words.length==3)
				features.incrementCount("NUM_WORDS_3", 1.0);
			else if (words.length==5)
				features.incrementCount("NUM_WORDS_5", 1.0);
			if (words.length<=3)
				features.incrementCount("NUM_WORDS_LESS_3", 1.0);
			else if (words.length>=7)
				features.incrementCount("NUM_WORDS_GREATER_7", 1.0);
			
			for (int i=0; i<words.length; i++) {
				String word = words[i].toLowerCase();
				features.incrementCount("WORD-" + word, 1.0);
			}
			String pattern = "[0-9].[0-9]";
		    Pattern r = Pattern.compile(pattern);
		    Matcher m = r.matcher(name);
			if (m.find())
				features.incrementCount("NUM_DECIMAL", 1.0);
			pattern = "[Ll]\\W?[Ll]\\W?[Cc]";
			r = Pattern.compile(pattern);
			m = r.matcher(name);
			if (m.find())
				features.incrementCount("LLC", 1.0);
			pattern = "\\s[Ii]nves";
			r = Pattern.compile(pattern);
			m = r.matcher(name);
			if (m.find())
				features.incrementCount("INVEST-", 1.0);
			
			features.incrementCount("BIAS_TERM", 1.0);
			return features;
		}
	}
	
	private static List<Character> stringToList (String word) {
		char[] characters = word.toCharArray();
		ArrayList<Character> charList = new ArrayList<Character>();
		for (char c : characters) {
			Character cObj = Character.valueOf(c);
			charList.add(cObj);
		}
		return charList;
	}

	private static List<LabeledInstance<String, String>> loadData(
			String fileName) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(fileName));
		List<LabeledInstance<String, String>> labeledInstances = new ArrayList<LabeledInstance<String, String>>();
		while (reader.ready()) {
			String line = reader.readLine();
			String[] parts = line.split("\t");
			String label = parts[0];
			String name = parts[1];
			LabeledInstance<String, String> labeledInstance = new LabeledInstance<String, String>(
					label, name);
			labeledInstances.add(labeledInstance);
		}
		reader.close();
		return labeledInstances;
	}
	
	public static List<LabeledInstance<String, String>> convertPOSData(List<POSTaggerTester.TaggedSentence> 
			sentenceData) {
		List<LabeledInstance<String, String>> labeledInstances = new ArrayList<LabeledInstance<String, String>>();
		for (POSTaggerTester.TaggedSentence sentence : sentenceData) {
			List<String> words = sentence.getWords();
			List<String> tags = sentence.getTags();
			for (int i=0; i<words.size(); i++) {
				LabeledInstance<String, String> labeledInstance = new LabeledInstance<String, String>(
						tags.get(i), words.get(i));
				labeledInstances.add(labeledInstance);
			}
		}
		return labeledInstances;
	}
	
	private static void testClassifier(
			ProbabilisticClassifier<String, String> classifier,
			List<LabeledInstance<String, String>> testData, boolean verbose) {
		double numCorrect = 0.0;
		double numTotal = 0.0;
		String sampleInput = testData.get(0).getInput();
		int numClasses = classifier.getProbabilities(sampleInput).size();
		List<String> labelSet = new ArrayList<String>(
				classifier.getProbabilities(sampleInput).keySet());
		Counter<Pair<String, String>> confusionMatrix = new Counter<Pair<String, String>>();
		for (LabeledInstance<String, String> testDatum : testData) {
			String name = testDatum.getInput();
			String label = classifier.getLabel(name);
			double confidence = classifier.getProbabilities(name).getCount(
					label);
			if (label.equals(testDatum.getLabel())) {
				numCorrect += 1.0;
			} 
			if (verbose) {
				boolean correct = label.equals(testDatum.getLabel());
				System.err.println(correct+"\t"+label+"\t"+testDatum.getLabel()+
						"\t"+confidence);
			}
			//else {
			//	if (verbose) {
					// display an error
			//		System.err.println("Error:\t" + name + "\tguess=\t" + label
			//				+ "\tgold=\t" + testDatum.getLabel() + "\tconfidence=\t"
			//				+ confidence);
			//	}
			//}
			confusionMatrix.incrementCount(Pair.makePair(label,testDatum.getLabel()), 1.0);
			numTotal += 1.0;
		}
		double accuracy = numCorrect / numTotal;
		System.out.println("Accuracy: " + accuracy);
		
		List<Pair<String, String>> labelPairs = 
				new ArrayList<Pair<String, String>>();
		for (String cLabel : labelSet) {
			for (String cLabel2 : labelSet) {
				Pair<String, String> pair = Pair.makePair(cLabel, cLabel2);
				labelPairs.add(pair);
			}
		}

		Pair.LexicographicPairComparator<String, String> c = new Pair.
				LexicographicPairComparator<String, String> (
						String.CASE_INSENSITIVE_ORDER, String.CASE_INSENSITIVE_ORDER);
		Collections.sort(labelPairs, c);
		
		double[][] confusionArray = new double[numClasses][numClasses];
		double[] labelTotal = new double[numClasses];
		int i, j, k = 0;
		for (Pair<String,String> pair : labelPairs) {
			i = k % numClasses;
			j = k / numClasses;
			confusionArray[i][j] = confusionMatrix.getCount(pair);
			labelTotal[i] += confusionMatrix.getCount(pair);
			k++;
		}
		printConfusionMatrix(labelPairs, confusionArray, labelTotal, numTotal);
	}
	
	public static void printConfusionMatrix(List<Pair<String, String>> labelPairs,
			double[][] confusionArray, double[] rowTotals, double numTotal) {
		int numClasses = rowTotals.length;
		System.out.println("\t\t Predicted Labels");
		for (int i=0; i<numClasses; i++)
			System.out.print("\t"+ labelPairs.get(numClasses*i).getFirst());
		System.out.println();
		for (int i=0; i<numClasses; i++) {
			System.out.print(labelPairs.get(i).getSecond());
			double rowTotal = rowTotals[i];
			for (int j=0; j<numClasses; j++) {
				double count = confusionArray[i][j];
				System.out.print("\t"+(int)count+"/"+(int)rowTotal);
			}
			System.out.println();
		}
	}

	public static void main(String[] args) throws IOException {
		// Parse command line flags and arguments
		Map<String, String> argMap = CommandLineUtils
				.simpleCommandLineParser(args);

		// Set up default parameters and settings
		String basePath = ".";
		String model = "baseline";
		boolean verbose = false;
		boolean useValidation = true;
		
		PrintStream out = new PrintStream(new FileOutputStream("hw2output.txt"));
		System.setErr(out);

		// Update defaults using command line specifications

		// The path to the assignment data
		if (argMap.containsKey("-path")) {
			basePath = argMap.get("-path");
		}
		System.out.println("Using base path: " + basePath);

		// A string descriptor of the model to use
		if (argMap.containsKey("-model")) {
			model = argMap.get("-model");
		}
		System.out.println("Using model: " + model);

		// A string descriptor of the model to use
		if (argMap.containsKey("-test")) {
			String testString = argMap.get("-test");
			if (testString.equalsIgnoreCase("test"))
				useValidation = false;
		}
		System.out.println("Testing on: "
				+ (useValidation ? "validation" : "test"));

		// Whether or not to print the individual speech errors.
		if (argMap.containsKey("-verbose")) {
			verbose = true;
		}

		// Load training, validation, and test data
		List<LabeledInstance<String, String>> trainingData = loadData(basePath
				+ "/pnp-train.txt");
		List<LabeledInstance<String, String>> validationData = loadData(basePath
				+ "/pnp-validate.txt");
		List<LabeledInstance<String, String>> testData = loadData(basePath
				+ "/pnp-test.txt");

		// Learn a classifier
		ProbabilisticClassifier<String, String> classifier = null;
		if (model.equalsIgnoreCase("baseline")) {
			classifier = new MostFrequentLabelClassifier.Factory<String, String>()
					.trainClassifier(trainingData);
		} else if (model.equalsIgnoreCase("n-gram")) {
			// TODO: construct your n-gram model here
		} else if (model.equalsIgnoreCase("maxent")) {
			// TODO: construct your maxent model here
			ProbabilisticClassifierFactory<String, String> factory = new MaximumEntropyClassifier.Factory<String, String, 
					String>(.7, 100, new ProperNameFeatureExtractor());
			long startTime = System.currentTimeMillis();
			classifier = factory.trainClassifier(trainingData);
			long endTime = System.currentTimeMillis();
			System.out.println("total took "+(endTime-startTime)/1000+" sec");
		} else if (model.equalsIgnoreCase("perceptron")) {
			ProbabilisticClassifierFactory<String, String> factory = new PerceptronClassifier.Factory<String, String, 
					String>(100, true, true, new ProperNameFeatureExtractor());
			long startTime = System.currentTimeMillis();
			classifier = factory.trainClassifier(trainingData);
			long endTime = System.currentTimeMillis();
			System.out.println("total took "+(endTime-startTime)/1000+" sec");
		} else if (model.equalsIgnoreCase("ngram")) {
			ProbabilisticClassifierFactory<String, String> factory = new CharacterNGramClassifier.Factory<String, 
					String>(0.5, 0.4, "");
			long startTime = System.currentTimeMillis();
			classifier = factory.trainClassifier(trainingData);
			long endTime = System.currentTimeMillis();
			System.out.println("total took "+(endTime-startTime)+" msec");
		}
		else {
			throw new RuntimeException("Unknown model descriptor: " + model);
		}

		// Test classifier
		testClassifier(classifier, (useValidation ? validationData : testData),
				verbose);
	}
}
