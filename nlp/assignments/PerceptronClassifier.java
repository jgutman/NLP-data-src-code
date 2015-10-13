package nlp.assignments;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import nlp.classify.*;
import nlp.math.DoubleArrays;
import nlp.util.Counter;	
import nlp.util.Indexer;
import nlp.util.Pair;

public class PerceptronClassifier<I, F, L> implements 
		ProbabilisticClassifier<I, L> {
	
	public static class Factory<I,F,L> extends
			MaximumEntropyClassifier.Factory<I,F,L> {
		
		private boolean average;
		private boolean shuffle;
		
		public ProbabilisticClassifier<I, L> trainClassifier(
				List<LabeledInstance<I, L>> trainingData) {
			
			// build data encodings so the inner loops can be efficient
			MaximumEntropyClassifier.Encoding<F, L> encoding = 
					super.buildEncoding(trainingData);
			MaximumEntropyClassifier.IndexLinearizer indexLinearizer = 
					super.buildIndexLinearizer(encoding);
			double[] initialWeights = super.buildInitialWeights(indexLinearizer);
			PerceptronClassifier<I,F,L> classifier = new PerceptronClassifier<I,F,L>
				(initialWeights, encoding, indexLinearizer, featureExtractor);
			iterateData(classifier, iterations, trainingData);
			return classifier;	
		}
		
		private void iterateData(PerceptronClassifier<I,F,L> 
				classifier, int iterations,
				List<LabeledInstance<I, L>> trainingData) {
			
			double [] averageWeights = new double[classifier.weights.length];
			double c = 1.0;
			
			for (int round = 0; round < iterations; round++) {
				if (shuffle)
					Collections.shuffle(trainingData);
				for (LabeledInstance<I, L> instance : trainingData) {
					I input = instance.getInput();
					L predictedLabel = classifier.getLabel(input);
					L trueLabel = instance.getLabel();
					if (!predictedLabel.equals(trueLabel)) {
						// update weights if guess wrong
						averageWeights = classifier.updateWeights(predictedLabel, trueLabel, 
								average, averageWeights, c);
					}
					c++;
				}
			}
			if (average) {
				for (int i=0; i<averageWeights.length; i++) {
					double w0 = classifier.weights[i];
					double wAvg = averageWeights[i];
					classifier.weights[i] = w0 - (wAvg/c);
				}
			}
		}
		
		public Factory(int iterations, boolean average, boolean shuffle,
				FeatureExtractor<I, F> featureExtractor) {
			super(1.0, iterations, featureExtractor);
			this.average = average;
			this.shuffle = shuffle;
		}
	}
	
	public double[] weights;
	private MaximumEntropyClassifier.Encoding<F, L> encoding;
	private MaximumEntropyClassifier.IndexLinearizer indexLinearizer;
	private FeatureExtractor<I, F> featureExtractor;
	private MaximumEntropyClassifier.EncodedDatum activeDatum;
	
	private static <F, L> double[] getScore(MaximumEntropyClassifier.EncodedDatum 
			datum, double[] weights, MaximumEntropyClassifier.Encoding<F, L> 
			encoding, MaximumEntropyClassifier.IndexLinearizer indexLinearizer) {
		
		int activeFeatures = datum.getNumActiveFeatures();
		int numLabels = encoding.getNumLabels();
		double[] score = new double[numLabels];
		
		for (int i=0; i<activeFeatures; i++) {
			int featureIndex = datum.getFeatureIndex(i);
			double fCount = datum.getFeatureCount(i);
			
			for (int j=0; j<numLabels; j++) {
				int weightIndex = indexLinearizer.getLinearIndex(featureIndex, j);
				score[j] += weights[weightIndex] * fCount;
			}
		}
		return score;
	}

	public Counter<L> getProbabilities(I input) {
		FeatureVector<F> featureVector = new BasicFeatureVector<F>(
				featureExtractor.extractFeatures(input));
		MaximumEntropyClassifier.EncodedDatum datum = 
				MaximumEntropyClassifier.EncodedDatum.encodeDatum(
						featureVector, encoding);
		this.activeDatum = datum;
		return getProbabilities(datum);
	}
	
	private Counter<L> getProbabilities(MaximumEntropyClassifier.EncodedDatum datum) {
		double[] scores = getScore(datum, weights, encoding, indexLinearizer);
		Counter<L> probabilityCounter = new Counter<L>();
		for (int labelIndex = 0; labelIndex < scores.length; labelIndex++) {
			double score = scores[labelIndex];
			L label = encoding.getLabel(labelIndex);
			probabilityCounter.setCount(label, score);
		}
		return probabilityCounter;
	}
	
	public L getLabel(I input) {
		return getProbabilities(input).argMax();
	}
	
	public double[] updateWeights(L predLabel, L trueLabel,
			boolean average, double[] averageWeights, double c) {
		int activeFeatures = activeDatum.getNumActiveFeatures();
		int correct = encoding.getLabelIndex(trueLabel);
		int wrong = encoding.getLabelIndex(predLabel);
		
		for (int i=0; i<activeFeatures; i++) {
			int featureIndex = activeDatum.getFeatureIndex(i);
			double fCount = activeDatum.getFeatureCount(i);
			
			int wtIndexCorrect = indexLinearizer.getLinearIndex(
					featureIndex, correct);
			int wtIndexWrong = indexLinearizer.getLinearIndex(
					featureIndex, wrong);
			weights[wtIndexCorrect] += fCount;
			weights[wtIndexWrong] -= fCount;
			
			if (average) {
				averageWeights[wtIndexCorrect] += c*fCount;
				averageWeights[wtIndexWrong] -= c*fCount;
			}
		}
		return averageWeights;
	}
	
	public PerceptronClassifier(double[] weights, 
			MaximumEntropyClassifier.Encoding<F,L> encoding,
			MaximumEntropyClassifier.IndexLinearizer indexLinearizer,
			FeatureExtractor<I, F> featureExtractor) {
		this.weights = weights;
		this.encoding = encoding;
		this.indexLinearizer = indexLinearizer;
		this.featureExtractor = featureExtractor;
	}
	
}