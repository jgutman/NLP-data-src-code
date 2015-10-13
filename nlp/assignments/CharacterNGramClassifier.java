package nlp.assignments;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.TreeMap;

import nlp.classify.*;
import nlp.math.DoubleArrays;
import nlp.util.Counter;	
import nlp.util.Indexer;
import nlp.util.Pair;
import nlp.langmodel.LanguageModel;
import nlp.util.CounterMap;

public class CharacterNGramClassifier<I, L> implements 
		ProbabilisticClassifier<I, L> {

	public static class Factory<I,L> implements
			ProbabilisticClassifierFactory<I, L> {
	
		String regex;
		double lambda1, lambda2;
		
		public ProbabilisticClassifier<I, L> trainClassifier(
				List<LabeledInstance<I, L>> trainingData) {
		
			Counter<L> classCounts = new Counter<L>();
			TreeMap<L, List<List<String>>> partitionedData = new TreeMap<L, List<List<String>>>();
			double nInstances = trainingData.size();
			
			for (LabeledInstance<I, L> instance: trainingData) {
				String input = (String)instance.getInput();
				L label = instance.getLabel();
				classCounts.incrementCount(label, 1.0);
				
				List<String> charByChar = Arrays.asList(input.split(regex));
				if (partitionedData.containsKey(label))
					partitionedData.get(label).add(charByChar);
				else {
					List<List<String>> examples= new ArrayList<List<String>>();
					examples.add(charByChar);
					partitionedData.put(label, examples);
				}
			}
			
			int numClasses = partitionedData.size();
			classCounts.scale(1.0/nInstances);
			
			TreeMap<L, EmpiricalTrigramLanguageModel> langModelsByClass = new TreeMap<L, EmpiricalTrigramLanguageModel>();
			
			for (L label : partitionedData.keySet()) {
				List<List<String>> classExamples = partitionedData.get(label);
				EmpiricalTrigramLanguageModel trigramModel = new EmpiricalTrigramLanguageModel(
						lambda1, lambda2, classExamples);
				langModelsByClass.put(label, trigramModel);
			}
			
			CharacterNGramClassifier<I, L> ngramClassifier = new CharacterNGramClassifier<I, L>(
					langModelsByClass, classCounts, regex);
			
			return ngramClassifier;
		}
		
		public Factory(double lambda1, double lambda2, String regex) {
			this.regex = regex;
			this.lambda1 = lambda1;
			this.lambda2 = lambda2;
		}
	}
	
	TreeMap<L, EmpiricalTrigramLanguageModel> langModelsByClass;
	Counter<L> classCounts;
	List<String> activeDatum;
	String regex;
	
	public Counter<L> getProbabilities(I input) {
		String inputCast = (String) input;
		List<String> charByChar = Arrays.asList(inputCast.split(regex));
		activeDatum = charByChar;
		
		Counter<L> probabilities = new Counter<L>();
		for (L label : classCounts.keySet()) {
			EmpiricalTrigramLanguageModel lm = langModelsByClass.get(label);
			double Py = classCounts.getCount(label);
			double PxGivenY = lm.getSentenceProbability(activeDatum);
			probabilities.setCount(label, Py*PxGivenY);
		}
		probabilities.normalize();
		return probabilities;
	}
	
	public L getLabel(I input) {
		return getProbabilities(input).argMax();
	}
	
	public CharacterNGramClassifier(TreeMap<L, EmpiricalTrigramLanguageModel> langModelsByClass,
			Counter<L> classCounts, String regex) {
		this.langModelsByClass = langModelsByClass;
		this.classCounts = classCounts;
		this.regex = regex;
	}

}