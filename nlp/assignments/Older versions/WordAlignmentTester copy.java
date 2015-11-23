package nlp.assignments;


import java.util.*;
import java.io.*;

import nlp.io.IOUtils;
import nlp.util.*;
import java.nio.charset.StandardCharsets;

/**
 * Harness for testing word-level alignments.  The code is hard-wired for the
 * alignment source to be English and the alignment target to be French (recall
 * that's the direction for translating INTO English in the noisy channel
 * model).
 *
 * Your projects will implement several methods of word-to-word alignment.
 */
public class WordAlignmentTester {

  static final String ENGLISH_EXTENSION = "e";
  static final String FRENCH_EXTENSION = "f";

  /**
   * A holder for a pair of sentences, each a list of strings.  Sentences in
   * the test sets have integer IDs, as well, which are used to retreive the
   * gold standard alignments for those sentences.
   */
  public static class SentencePair {
    int sentenceID;
    String sourceFile;
    List<String> englishWords;
    List<String> frenchWords;

    public int getSentenceID() {
      return sentenceID;
    }

    public String getSourceFile() {
      return sourceFile;
    }

    public List<String> getEnglishWords() {
      return englishWords;
    }

    public List<String> getFrenchWords() {
      return frenchWords;
    }

    public String toString() {
      StringBuilder sb = new StringBuilder();
      for (int englishPosition = 0; englishPosition < englishWords.size(); englishPosition++) {
        String englishWord = englishWords.get(englishPosition);
        sb.append(englishPosition);
        sb.append(":");
        sb.append(englishWord);
        sb.append(" ");
      }
      sb.append("\n");
      for (int frenchPosition = 0; frenchPosition < frenchWords.size(); frenchPosition++) {
        String frenchWord = frenchWords.get(frenchPosition);
        sb.append(frenchPosition);
        sb.append(":");
        sb.append(frenchWord);
        sb.append(" ");
      }
      sb.append("\n");
      return sb.toString();
    }

    public SentencePair(int sentenceID, String sourceFile, List<String> englishWords, List<String> frenchWords) {
      this.sentenceID = sentenceID;
      this.sourceFile = sourceFile;
      this.englishWords = englishWords;
      this.frenchWords = frenchWords;
    }
  }

  /**
   * Alignments serve two purposes, both to indicate your system's guessed
   * alignment, and to hold the gold standard alignments.  Alignments map index
   * pairs to one of three values, unaligned, possibly aligned, and surely
   * aligned.  Your alignment guesses should only contain sure and unaligned
   * pairs, but the gold alignments contain possible pairs as well.
   *
   * To build an alignment, start with an empty one and use
   * addAlignment(i,j,true).  To display one, use the render method.
   */
  public static class Alignment {
    Set<Pair<Integer, Integer>> sureAlignments;
    Set<Pair<Integer, Integer>> possibleAlignments;

    public boolean containsSureAlignment(int englishPosition, int frenchPosition) {
      return sureAlignments.contains(new Pair<Integer, Integer>(englishPosition, frenchPosition));
    }

    public boolean containsPossibleAlignment(int englishPosition, int frenchPosition) {
      return possibleAlignments.contains(new Pair<Integer, Integer>(englishPosition, frenchPosition));
    }

    public void addAlignment(int englishPosition, int frenchPosition, boolean sure) {
      Pair<Integer, Integer> alignment = new Pair<Integer, Integer>(englishPosition, frenchPosition);
      if (sure)
        sureAlignments.add(alignment);
      possibleAlignments.add(alignment);
    }

    public Alignment() {
      sureAlignments = new HashSet<Pair<Integer, Integer>>();
      possibleAlignments = new HashSet<Pair<Integer, Integer>>();
    }

    public static String render(Alignment alignment, SentencePair sentencePair) {
      return render(alignment, alignment, sentencePair);
    }

    public static String render(Alignment reference, Alignment proposed, SentencePair sentencePair) {
      StringBuilder sb = new StringBuilder();
      for (int frenchPosition = 0; frenchPosition < sentencePair.getFrenchWords().size(); frenchPosition++) {
        for (int englishPosition = 0; englishPosition < sentencePair.getEnglishWords().size(); englishPosition++) {
          boolean sure = reference.containsSureAlignment(englishPosition, frenchPosition);
          boolean possible = reference.containsPossibleAlignment(englishPosition, frenchPosition);
          char proposedChar = ' ';
          if (proposed.containsSureAlignment(englishPosition, frenchPosition))
            proposedChar = '#';
          if (sure) {
            sb.append('[');
            sb.append(proposedChar);
            sb.append(']');
          } else {
            if (possible) {
              sb.append('(');
              sb.append(proposedChar);
              sb.append(')');
            } else {
              sb.append(' ');
              sb.append(proposedChar);
              sb.append(' ');
            }
          }
        }
        sb.append("| ");
        sb.append(sentencePair.getFrenchWords().get(frenchPosition));
        sb.append('\n');
      }
      for (int englishPosition = 0; englishPosition < sentencePair.getEnglishWords().size(); englishPosition++) {
        sb.append("---");
      }
      sb.append("'\n");
      boolean printed = true;
      int index = 0;
      while (printed) {
        printed = false;
        StringBuilder lineSB = new StringBuilder();
        for (int englishPosition = 0; englishPosition < sentencePair.getEnglishWords().size(); englishPosition++) {
          String englishWord = sentencePair.getEnglishWords().get(englishPosition);
          if (englishWord.length() > index) {
            printed = true;
            lineSB.append(' ');
            lineSB.append(englishWord.charAt(index));
            lineSB.append(' ');
          } else {
            lineSB.append("   ");
          }
        }
        index += 1;
        if (printed) {
          sb.append(lineSB);
          sb.append('\n');
        }
      }
      return sb.toString();
    }
  }

  /**
   * WordAligners have one method: alignSentencePair, which takes a sentence
   * pair and produces an alignment which specifies an english source for each
   * french word which is not aligned to "null".  Explicit alignment to
   * position -1 is equivalent to alignment to "null".
   */
  static interface WordAligner {
    Alignment alignSentencePair(SentencePair sentencePair);
  }

  /**
   * Simple alignment baseline which maps french positions to english positions.
   * If the french sentence is longer, all final word map to null.
   */
  static class BaselineWordAligner implements WordAligner {
    public Alignment alignSentencePair(SentencePair sentencePair) {
      Alignment alignment = new Alignment();
      int numFrenchWords = sentencePair.getFrenchWords().size();
      int numEnglishWords = sentencePair.getEnglishWords().size();
      for (int frenchPosition = 0; frenchPosition < numFrenchWords; frenchPosition++) {
        int englishPosition = frenchPosition;
        if (englishPosition >= numEnglishWords)
          englishPosition = -1;
        alignment.addAlignment(englishPosition, frenchPosition, true);
      }
      return alignment;
    }
  }
  
  /**
   * Simple heuristic model where each french word f is assigned to the english word e that maximizes the ratio
   * c(f,e)/[c(e)*c(f)]. Map French word to BASELINE if c(f,e) = 0 for all English words in paired sentence.
   */
  static class HeuristicWordAligner implements WordAligner {
	  List<SentencePair> trainingSentencePairs;
	  Counter<String> fCounts;
	  Counter<String> eCounts;
	  CounterMap<String,String> collocationCounts;
	  
	  public Alignment alignSentencePair(SentencePair sentencePair) {
		  Alignment alignment = new Alignment();
	      List<String> frenchWords = sentencePair.getFrenchWords();
	      List<String> englishWords = sentencePair.getEnglishWords();     
	      int numFrenchWords = frenchWords.size();
	      int numEnglishWords = englishWords.size();
	      
	      for (int frenchPosition = 0; frenchPosition < numFrenchWords; frenchPosition++) {
	    	  String f = frenchWords.get(frenchPosition);
	    	  int englishMaxPosition = frenchPosition;
	    	  if (englishMaxPosition >= numEnglishWords)
	    		  englishMaxPosition = -1; // map French word to BASELINE if c(f,e) = 0 for all English words
	    	  double maxConditionalProb = 0;
	    	  for (int englishPosition = 0; englishPosition < numEnglishWords; englishPosition++) {
	    		  String e = englishWords.get(englishPosition);
	    		  double conditionalGivenEnglish = collocationCounts.getCount(f, e) / (eCounts.getCount(e));
	    		  if (conditionalGivenEnglish > maxConditionalProb) {
	    			  maxConditionalProb = conditionalGivenEnglish;
	    			  englishMaxPosition = englishPosition;
	    		  }
	    	  }	
	    	  alignment.addAlignment(englishMaxPosition, frenchPosition, true);
	      }
		  return alignment;
	  }
	  
	  private void trainCounters() {
		  for (SentencePair sentencePair : trainingSentencePairs) {
			  List<String> frenchWords = sentencePair.getFrenchWords();
		      List<String> englishWords = sentencePair.getEnglishWords();
		      
		      //fCounts.incrementAll(frenchWords, 1.0); // won't affect the argMax
		      eCounts.incrementAll(englishWords, 1.0);
		      
		      for (String f: frenchWords) {
		    	  for (String e: englishWords)
		    		  collocationCounts.incrementCount(f, e, 1.0);
		      }
		  }
		  System.out.println("Trained!");
	  }
	  
	  public HeuristicWordAligner(List<SentencePair> data) {
		  this.trainingSentencePairs = data;
		  this.fCounts = new Counter<String>();
		  this.eCounts = new Counter<String>();
		  this.collocationCounts = new CounterMap<String,String>();
		  trainCounters();
	  }
  }
  
  /**
   * Simple heuristic model to align words by maximizing the Dice coefficient.
   */
  static class DiceWordAligner implements WordAligner {
	  List<SentencePair> trainingSentencePairs;
	  Counter<String> fCountSentences;
	  Counter<String> eCountSentences;
	  CounterMap<String,String> collocationCountSentences; 
	  
	  public Alignment alignSentencePair(SentencePair sentencePair) {
		  Alignment alignment = new Alignment();
	      List<String> frenchWords = sentencePair.getFrenchWords();
	      List<String> englishWords = sentencePair.getEnglishWords();
	      int numFrenchWords = frenchWords.size();
	      int numEnglishWords = englishWords.size();
	      
	      for (int frenchPosition = 0; frenchPosition < numFrenchWords; frenchPosition++) {
	    	  String f = frenchWords.get(frenchPosition);
	    	  int englishMaxPosition = frenchPosition;
	    	  if (englishMaxPosition >= numEnglishWords)
	    		  englishMaxPosition = -1; // map French word to BASELINE if c(f,e) = 0 for all English words
	    	  double maxDice = 0;
	    	  for (int englishPosition = 0; englishPosition < numEnglishWords; englishPosition++) {
	    		  String e = englishWords.get(englishPosition);
	    		  double dice = getDiceCoefficient(f,e);
	    		  if (dice > maxDice) {
	    			  maxDice = dice;
	    			  englishMaxPosition = englishPosition;
	    		  }
	    	  }	
	    	  alignment.addAlignment(englishMaxPosition, frenchPosition, true);
	      }
		  return alignment;
	  }
	  
	  private void trainCounters() {
		  for (SentencePair sentencePair : trainingSentencePairs) {
			  List<String> frenchWords = sentencePair.getFrenchWords();
		      List<String> englishWords = sentencePair.getEnglishWords();
		      Set<String> frenchSet = new HashSet<String>(frenchWords);
		      Set<String> englishSet = new HashSet<String>(englishWords);
		      
		      fCountSentences.incrementAll(frenchSet, 1.0); 
		      eCountSentences.incrementAll(englishSet, 1.0);
		      
		      for (String f: frenchSet) {
		    	  for (String e: englishSet)
		    		  collocationCountSentences.incrementCount(f, e, 1.0);
		      }
		  }
		  System.out.println("Trained!");
	  }
	  
	  private double getDiceCoefficient(String f, String e) {
		  double intersection = collocationCountSentences.getCount(f,e);
		  double cardinalityF = fCountSentences.getCount(f);
		  double cardinalityE = eCountSentences.getCount(e);
		  
		  double dice = 2*intersection / (cardinalityF + cardinalityE);
		  return dice;
	  }
	  
	  public DiceWordAligner(List<SentencePair> data) {
		  this.trainingSentencePairs = data;
		  this.fCountSentences = new Counter<String>();
		  this.eCountSentences = new Counter<String>();
		  this.collocationCountSentences = new CounterMap<String,String>();
		  trainCounters();
	  }
  }

  static class IBMmodel1WordAligner implements WordAligner {
	  List<SentencePair> trainingSentencePairs;
	  CounterMap<String,String> translationProbs;
	  boolean initialize;
	  static final String NULL = "<NULL>";
	  
	  public Alignment alignSentencePair(SentencePair sentencePair) {
		  Alignment alignment = new Alignment();
		  List<String> frenchWords = sentencePair.getFrenchWords();
	      List<String> englishWords = sentencePair.getEnglishWords();     
	      int numFrenchWords = frenchWords.size();
	      int numEnglishWords = englishWords.size();
	      
		  // Model 1 assumes all alignments are equally likely
	      // So we can just take the argMax of t(f|e) to get the englishMaxPosition
	      for (int frenchPosition = 0; frenchPosition < numFrenchWords; frenchPosition++) {
	    	  String f = frenchWords.get(frenchPosition);
	    	  int englishMaxPosition = -1;
	    	  double maxTranslationProb = translationProbs.getCount(f, NULL);
	    	  for (int englishPosition = 0; englishPosition < numEnglishWords; englishPosition++) {
	    		  String e = englishWords.get(englishPosition);
	    		  double translationProb = translationProbs.getCount(f, e);
	    		  if (translationProb > maxTranslationProb) {
	    			  maxTranslationProb = translationProb;
	    			  englishMaxPosition = englishPosition;
	    		  }
	    	  }
	    	  alignment.addAlignment(englishMaxPosition, frenchPosition, true);
	      }
		  return alignment;
	  }
	  
	  private CounterMap<String,String> trainEM(int maxIterations) {
		  Set<String> englishVocab = new HashSet<String>();
		  Set<String> frenchVocab = new HashSet<String>();
		  
		  CounterMap<String,String> translations = new CounterMap<String,String>();
		  englishVocab.add(NULL);
		  boolean converged = false;
		  int iteration = 0;
		  final double thresholdProb = 0.0001;
		  
		  for (SentencePair sentencePair : trainingSentencePairs) {
			  List<String> frenchWords = sentencePair.getFrenchWords();
			  List<String> englishWords = sentencePair.getEnglishWords();
			  // add words from list to vocabulary sets
			  englishVocab.addAll(englishWords);
			  frenchVocab.addAll(frenchWords);
		  }
		  System.out.println("Ready");
		  
		  // We need to initialize translations.getCount(f,e) uniformly
		  // t(f|e) summed over all f in {F} = 1
		  final double initialCount = 1.0 / frenchVocab.size();
		  
		  while(iteration < maxIterations) {
			  CounterMap<String,String> counts = new CounterMap<String,String>(); // set count(f|e) to 0 for all e,f
			  Counter<String> totalEnglish = new Counter<String>(); // set total(e) to 0 for all e
			  
			  // E-step: loop over all sentences and update counts
			  for (SentencePair sentencePair : trainingSentencePairs) {
				  List<String> frenchWords = sentencePair.getFrenchWords();
				  List<String> englishWords = sentencePair.getEnglishWords();
				  
			      int numFrenchWords = frenchWords.size();
			      int numEnglishWords = englishWords.size();
			      Counter<String> sTotalF = new Counter<String>(); 
			      
			      // compute normalization constant sTotalF
			      for (int frenchPosition = 0; frenchPosition < numFrenchWords; frenchPosition++) {
			    	  String f = frenchWords.get(frenchPosition);
			    	  // initialize and compute for English = NULL
			    	  if (!translations.containsKey(f) && initialize)
			    		  translations.setCount(f, NULL, initialCount);
			    	  else if (!translations.containsKey(f))
			    		  translations.setCount(f, NULL, thresholdProb);
			    	  sTotalF.incrementCount(f, translations.getCount(f, NULL)); 
			    	  for (int englishPosition = 0; englishPosition < numEnglishWords; englishPosition++) {
			    		  String e = englishWords.get(englishPosition);
			    		  if (!(translations.getCounter(f)).containsKey(e) && initialize)
			    			  translations.setCount(f, e, initialCount);
			    		  else if (!(translations.getCounter(f)).containsKey(e))
			    			  translations.setCount(f, e, thresholdProb);
			    		  sTotalF.incrementCount(f, translations.getCount(f, e));
			    	  }
			      }
			      
			      // collect counts in counts and totalEnglish
			      for (int frenchPosition = 0; frenchPosition < numFrenchWords; frenchPosition++) {
			    	  String f = frenchWords.get(frenchPosition);
			    	  
			    	  // collect counts for English = NULL
			    	  double count = translations.getCount(f, NULL) / sTotalF.getCount(f);
			    	  counts.incrementCount(NULL, f, count);
			    	  totalEnglish.incrementCount(NULL, count);
			    	  for (int englishPosition = 0; englishPosition < numEnglishWords; englishPosition++) {
			    		  String e = englishWords.get(englishPosition);
			    		  count = translations.getCount(f, e) / sTotalF.getCount(f);
			    		  counts.incrementCount(e, f, count);
			    		  totalEnglish.incrementCount(e, count);
			    	  }
			      }
			  } // end of E-step
			  System.out.println("Completed E-step");
			  
			  // M-step: update probabilities with counts from E-step and check for convergence
			  iteration++;
			  
			  // Maybe this isn't right and we need to update the transitions for zero counts by removing the entries from the 
			  // translations counterMap? i.e. just create a new translations counterMap in this step in place of the old one
			  CounterMap<String, String> updateTranslations - new CounterMap<String, String>();
			  // We want to reset all translations, don't want to carry over translations that should be zero
			  // Check E-step, we only want to initialize translations in the first iteration
			  
			  for (String e : counts.keySet()) {//englishVocab) {
				  double normalizer = totalEnglish.getCount(e);
				  for (String f : (counts.getCounter(e)).keySet()) {//frenchVocab) {
					  
					  // To speed implementation, we want to update translations only when count / normalizer > threshold
					  double prob = counts.getCount(e, f) / normalizer;
					  if (!initialize) {					  
						  if (prob > thresholdProb)
							  updateTranslations.setCount(f, e, prob);
						  else
							  (updateTranslations.getCounter(f)).removeKey(e);
					  }
					  else {
						  updateTranslations.setCount(f, e, prob);
					  }
				  }
			  }
			  translations = updateTranslations; 
			  System.out.println("Completed iteration " + iteration);
		  } // end of M-step
		  
		  System.out.println("Trained!");
		  return translations;
	  }
	  
	  // TODO: treat null alignment as special case, reserve constant probability mass for null alignments
	  public IBMmodel1WordAligner(List<SentencePair> data, int maxIterations, boolean initialize) {
		  this.trainingSentencePairs = data;
		  this.initialize = initialize;
		  this.translationProbs = trainEM(maxIterations);
	  }
  }
  
  static class IBMmodel2WordAligner implements WordAligner {
	  List<SentencePair> trainingSentencePairs;
	  CounterMap<String,String> translationProbs;
	  boolean initialize;
	  static final String NULL = "<NULL>";
	  
	  // TODO: implement model 2 with distance metric, alter alignSentencePair to find new argMax over P(a)*t(f|e)
	  public Alignment alignSentencePair(SentencePair sentencePair) {
		  Alignment alignment = new Alignment();
		  List<String> frenchWords = sentencePair.getFrenchWords();
	      List<String> englishWords = sentencePair.getEnglishWords();     
	      int numFrenchWords = frenchWords.size();
	      int numEnglishWords = englishWords.size();
	      
		  // Model 2 does not assume all alignments are equally likely
	      for (int frenchPosition = 0; frenchPosition < numFrenchWords; frenchPosition++) {
	    	  String f = frenchWords.get(frenchPosition);
	    	  int englishMaxPosition = -1;
	    	  double maxTranslationProb = translationProbs.getCount(f, NULL);
	    	  for (int englishPosition = 0; englishPosition < numEnglishWords; englishPosition++) {
	    		  String e = englishWords.get(englishPosition);
	    		  double translationProb = translationProbs.getCount(f, e);
	    		  if (translationProb > maxTranslationProb) {
	    			  maxTranslationProb = translationProb;
	    			  englishMaxPosition = englishPosition;
	    		  }
	    	  }
	    	  alignment.addAlignment(englishMaxPosition, frenchPosition, true);
	      }
		  return alignment;
	  }
	  
	  private double computeDistortionProb(int englishPosition, int frenchPosition, 
			  int numEnglishWords, int numFrenchWords) {
		  double alpha = 1.0;
		  double kNull = .2; // proportion of probability mass to allot for null alignments
		  if (englishPosition == 0) { // How to compute distortion probability for null alignments?
			  return kNull;
		  }
		  else {
			  double dist = englishPosition - (frenchPosition * numEnglishWords / numFrenchWords);
			  double metric = (1.0 - kNull) * Math.exp(-1.0 * alpha * dist);
			  return metric;
		  }		  
	  }
	  
	  private CounterMap<String,String> trainEM(int maxIterations) {
		  Set<String> englishVocab = new HashSet<String>();
		  Set<String> frenchVocab = new HashSet<String>();
		  
		  CounterMap<String,String> translations = new CounterMap<String,String>();
		  englishVocab.add(NULL);
		  int iteration = 0;
		  final double thresholdProb = 0.0001;
		  
		  for (SentencePair sentencePair : trainingSentencePairs) {
			  List<String> frenchWords = sentencePair.getFrenchWords();
			  List<String> englishWords = sentencePair.getEnglishWords();
			  // add words from list to vocabulary sets
			  englishVocab.addAll(englishWords);
			  frenchVocab.addAll(frenchWords);
		  }
		  System.out.println("Ready");
		  
		  // We need to initialize translations.getCount(f,e) uniformly
		  // t(f|e) summed over all e in {E + NULL} = 1
		  final double initialCount = 1.0 / englishVocab.size();
		  
		  while(iteration < maxIterations) {
			  CounterMap<String,String> counts = new CounterMap<String,String>(); // set count(f|e) to 0 for all e,f
			  Counter<String> totalEnglish = new Counter<String>(); // set total(e) to 0 for all e
			  
			  // E-step: loop over all sentences and update counts
			  for (SentencePair sentencePair : trainingSentencePairs) {
				  List<String> frenchWords = sentencePair.getFrenchWords();
				  List<String> englishWords = sentencePair.getEnglishWords();
				  
			      int numFrenchWords = frenchWords.size();
			      int numEnglishWords = englishWords.size();
			      Counter<String> sTotalF = new Counter<String>(); 
			      
			      // compute normalization constant sTotalF
			      for (int frenchPosition = 0; frenchPosition < numFrenchWords; frenchPosition++) {
			    	  String f = frenchWords.get(frenchPosition);
			    	  // initialize and compute for English = NULL
			    	  if (!translations.containsKey(f) && initialize)
			    		  translations.setCount(f, NULL, initialCount);
			    	  else if (!translations.containsKey(f))
			    		  translations.setCount(f, NULL, thresholdProb);
			    	  sTotalF.incrementCount(f, translations.getCount(f, NULL)); 
			    	  for (int englishPosition = 0; englishPosition < numEnglishWords; englishPosition++) {
			    		  String e = englishWords.get(englishPosition);
			    		  if (!(translations.getCounter(f)).containsKey(e) && initialize)
			    			  translations.setCount(f, e, initialCount);
			    		  else if (!(translations.getCounter(f)).containsKey(e))
			    			  translations.setCount(f, e, thresholdProb);
			    		  sTotalF.incrementCount(f, translations.getCount(f, e));
			    	  }
			      }
			      
			      // collect counts in counts and totalEnglish
			      for (int frenchPosition = 0; frenchPosition < numFrenchWords; frenchPosition++) {
			    	  String f = frenchWords.get(frenchPosition);
			    	  
			    	  // collect counts for English = NULL
			    	  double count = translations.getCount(f, NULL) / sTotalF.getCount(f);
			    	  counts.incrementCount(NULL, f, count);
			    	  totalEnglish.incrementCount(NULL, count);
			    	  for (int englishPosition = 0; englishPosition < numEnglishWords; englishPosition++) {
			    		  String e = englishWords.get(englishPosition);
			    		  count = translations.getCount(f, e) / sTotalF.getCount(f);
			    		  counts.incrementCount(e, f, count);
			    		  totalEnglish.incrementCount(e, count);
			    	  }
			      }
			  } // end of E-step
			  System.out.println("Completed E-step");
			  
			  // M-step: update probabilities with counts from E-step and check for convergence
			  iteration++;
			  for (String e : counts.keySet()) {//englishVocab) {
				  double normalizer = totalEnglish.getCount(e);
				  for (String f : (counts.getCounter(e)).keySet()) {//frenchVocab) {
					  
					  // To speed implementation, we want to update translations only when count / normalizer > threshold
					  double prob = counts.getCount(e, f) / normalizer;
					  if (!initialize) {					  
						  if (prob > thresholdProb)
							  translations.setCount(f, e, prob);
						  else
							  (translations.getCounter(f)).removeKey(e);
					  }
					  else {
						  translations.setCount(f, e, prob);
					  }
				  }
			  }
			  System.out.println("Completed iteration " + iteration);
		  } // end of M-step
		  
		  System.out.println("Trained!");
		  return translations;
	  }
	  
	  // TODO: treat null alignment as special case, reserve constant probability mass for null alignments
	  public IBMmodel2WordAligner(List<SentencePair> data, int maxIterations, boolean initialize) {
		  this.trainingSentencePairs = data;
		  this.initialize = initialize;
		  this.translationProbs = trainEM(maxIterations);
	  }
  }
  
  public static void main(String[] args) throws IOException {
    // Parse command line flags and arguments
    Map<String,String> argMap = CommandLineUtils.simpleCommandLineParser(args);

    // Set up default parameters and settings
    String basePath = ".";
    int maxTrainingSentences = 0;
    int maxIterations = 20;
    boolean verbose = false;
    boolean initialize = false;
    String dataset = "mini";
    String model = "baseline";

    // Update defaults using command line specifications
    if (argMap.containsKey("-path")) {
      basePath = argMap.get("-path");
      System.out.println("Using base path: "+basePath);
    }
    if (argMap.containsKey("-sentences")) {
      maxTrainingSentences = Integer.parseInt(argMap.get("-sentences"));
      System.out.println("Using an additional "+maxTrainingSentences+" training sentences.");
    }
    if (argMap.containsKey("-data")) {
      dataset = argMap.get("-data");
      System.out.println("Running with data: "+dataset);
    } else {
      System.out.println("No data set specified.  Use -data [miniTest, validate].");
    }
    if (argMap.containsKey("-model")) {
      model = argMap.get("-model");
      System.out.println("Running with model: "+model);
    } else {
      System.out.println("No model specified.  Use -model modelname.");
    }
    if (argMap.containsKey("-verbose")) {
      verbose = true;
    }
    if (argMap.containsKey("-iterations")) {
    	maxIterations = Integer.parseInt(argMap.get("-iterations"));
    }
    if (argMap.containsKey("-initialize")) {
    	initialize = true;
    }

    // Read appropriate training and testing sets.
    List<SentencePair> trainingSentencePairs = new ArrayList<SentencePair>();
    if (! (dataset.equals("miniTest") || dataset.equals("mini")) && maxTrainingSentences > 0)
      trainingSentencePairs = readSentencePairs(basePath+"/training", maxTrainingSentences);
    List<SentencePair> testSentencePairs = new ArrayList<SentencePair>();
    Map<Integer,Alignment> testAlignments = new HashMap<Integer, Alignment>();
    if (dataset.equalsIgnoreCase("validate")) {
      testSentencePairs = readSentencePairs(basePath+"/trial", Integer.MAX_VALUE);
      testAlignments = readAlignments(basePath+"/trial/trial.wa");
    } else if (dataset.equals("miniTest") || dataset.equals("mini")) {
      testSentencePairs = readSentencePairs(basePath+"/mini", Integer.MAX_VALUE);
      testAlignments = readAlignments(basePath+"/mini/mini.wa");
    } else {
      throw new RuntimeException("Bad data set mode: "+ dataset+", use validate or miniTest.");
    }
    trainingSentencePairs.addAll(testSentencePairs);

    // Build model
    WordAligner wordAligner = null;
    if (model.equalsIgnoreCase("baseline")) {
      wordAligner = new BaselineWordAligner();
    }
    // TODO : build other alignment models
    else if (model.equalsIgnoreCase("heuristic")) {
    	wordAligner = new HeuristicWordAligner(trainingSentencePairs);
    }
    else if (model.equalsIgnoreCase("dice")) {
    	wordAligner = new DiceWordAligner(trainingSentencePairs);
    }
    else if (model.equalsIgnoreCase("ibm1") || model.equalsIgnoreCase("ibmModel1")) {
    	wordAligner = new IBMmodel1WordAligner(trainingSentencePairs, maxIterations, initialize);
    }
    else if (model.equalsIgnoreCase("ibm2") || model.equalsIgnoreCase("ibmModel2")) {
    	wordAligner = new IBMmodel2WordAligner(trainingSentencePairs, maxIterations, initialize);
    }

    // Test model
    test(wordAligner, testSentencePairs, testAlignments, verbose);
    
    // Generate file for submission //can comment out if not ready for submission
    testSentencePairs = readSentencePairs(basePath+"/test", Integer.MAX_VALUE);
    predict(wordAligner, testSentencePairs, basePath+"/"+model+".out");
  }

  private static void test(WordAligner wordAligner, List<SentencePair> testSentencePairs, Map<Integer, Alignment> testAlignments, boolean verbose) {
    int proposedSureCount = 0;
    int proposedPossibleCount = 0;
    int sureCount = 0;
    int proposedCount = 0;
    for (SentencePair sentencePair : testSentencePairs) {
      Alignment proposedAlignment = wordAligner.alignSentencePair(sentencePair);
      Alignment referenceAlignment = testAlignments.get(sentencePair.getSentenceID());
      if (referenceAlignment == null)
        throw new RuntimeException("No reference alignment found for sentenceID "+sentencePair.getSentenceID());
      if (verbose) System.out.println("Alignment:\n"+Alignment.render(referenceAlignment,proposedAlignment,sentencePair));
      for (int frenchPosition = 0; frenchPosition < sentencePair.getFrenchWords().size(); frenchPosition++) {
        for (int englishPosition = 0; englishPosition < sentencePair.getEnglishWords().size(); englishPosition++) {
          boolean proposed = proposedAlignment.containsSureAlignment(englishPosition, frenchPosition);
          boolean sure = referenceAlignment.containsSureAlignment(englishPosition, frenchPosition);
          boolean possible = referenceAlignment.containsPossibleAlignment(englishPosition, frenchPosition);
          if (proposed && sure) proposedSureCount += 1;
          if (proposed && possible) proposedPossibleCount += 1;
          if (proposed) proposedCount += 1;
          if (sure) sureCount += 1;
        }
      }
    }
    System.out.println("Precision: "+proposedPossibleCount/(double)proposedCount);
    System.out.println("Recall: "+proposedSureCount/(double)sureCount);
    System.out.println("AER: "+(1.0-(proposedSureCount+proposedPossibleCount)/(double)(sureCount+proposedCount)));
  }

  private static void predict(WordAligner wordAligner, List<SentencePair> testSentencePairs, String path) throws IOException {
	BufferedWriter writer = new BufferedWriter(new FileWriter(path));
    for (SentencePair sentencePair : testSentencePairs) {
      Alignment proposedAlignment = wordAligner.alignSentencePair(sentencePair);
      for (int frenchPosition = 0; frenchPosition < sentencePair.getFrenchWords().size(); frenchPosition++) {
        for (int englishPosition = 0; englishPosition < sentencePair.getEnglishWords().size(); englishPosition++) {
          if (proposedAlignment.containsSureAlignment(englishPosition, frenchPosition)) {
        	writer.write(frenchPosition + "-" + englishPosition + " ");
          }
        }
      }
      writer.write("\n");
    }
    writer.close();
  }

  // BELOW HERE IS IO CODE

  private static Map<Integer, Alignment> readAlignments(String fileName) {
    Map<Integer,Alignment> alignments = new HashMap<Integer, Alignment>();
    try {
      BufferedReader in = new BufferedReader(new FileReader(fileName));
      while (in.ready()) {
        String line = in.readLine();
        String[] words = line.split("\\s+");
        if (words.length != 4)
          throw new RuntimeException("Bad alignment file "+fileName+", bad line was "+line);
        Integer sentenceID = Integer.parseInt(words[0]);
        Integer englishPosition = Integer.parseInt(words[1])-1;
        Integer frenchPosition = Integer.parseInt(words[2])-1;
        String type = words[3];
        Alignment alignment = alignments.get(sentenceID);
        if (alignment == null) {
          alignment = new Alignment();
          alignments.put(sentenceID, alignment);
        }
        alignment.addAlignment(englishPosition, frenchPosition, type.equals("S"));
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return alignments;
  }

  private static List<SentencePair> readSentencePairs(String path, int maxSentencePairs) {
    List<SentencePair> sentencePairs = new ArrayList<SentencePair>();
    List<String> baseFileNames = getBaseFileNames(path);
    for (String baseFileName : baseFileNames) {
      if (sentencePairs.size() >= maxSentencePairs)
        continue;
      sentencePairs.addAll(readSentencePairs(baseFileName));
    }
    return sentencePairs;
  }

  private static List<SentencePair> readSentencePairs(String baseFileName) {
    List<SentencePair> sentencePairs = new ArrayList<SentencePair>();
    String englishFileName = baseFileName + "." + ENGLISH_EXTENSION;
    String frenchFileName = baseFileName + "." + FRENCH_EXTENSION;
    try {
      BufferedReader englishIn = new BufferedReader(new FileReader(englishFileName));
      //BufferedReader frenchIn = new BufferedReader(new FileReader(frenchFileName));
      BufferedReader frenchIn = new BufferedReader(new InputStreamReader(
    		  new FileInputStream(frenchFileName), StandardCharsets.ISO_8859_1));
      while (englishIn.ready() && frenchIn.ready()) {
        String englishLine = englishIn.readLine();
        String frenchLine = frenchIn.readLine();
        Pair<Integer,List<String>> englishSentenceAndID = readSentence(englishLine);
        Pair<Integer,List<String>> frenchSentenceAndID = readSentence(frenchLine);
        if (! englishSentenceAndID.getFirst().equals(frenchSentenceAndID.getFirst()))
          throw new RuntimeException("Sentence ID confusion in file "+baseFileName+", lines were:\n\t"+englishLine+"\n\t"+frenchLine);
        sentencePairs.add(new SentencePair(englishSentenceAndID.getFirst(), baseFileName, englishSentenceAndID.getSecond(), frenchSentenceAndID.getSecond()));
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return sentencePairs;
  }

  private static Pair<Integer, List<String>> readSentence(String line) {
    int id = -1;
    List<String> words = new ArrayList<String>();
    String[] tokens = line.split("\\s+");
    for (int i = 0; i < tokens.length; i++) {
      String token = tokens[i];
      if (token.equals("<s")) continue;
      if (token.equals("</s>")) continue;
      if (token.startsWith("snum=")) {
        String idString = token.substring(5,token.length()-1);
        id = Integer.parseInt(idString);
        continue;
      }
      words.add(token.intern());
    }
    return new Pair<Integer, List<String>>(id, words);
  }

  private static List<String> getBaseFileNames(String path) {
    List<File> englishFiles = IOUtils.getFilesUnder(path, new FileFilter() {
      public boolean accept(File pathname) {
        if (pathname.isDirectory())
          return true;
        String name = pathname.getName();
        return name.endsWith(ENGLISH_EXTENSION);
      }
    });
    List<String> baseFileNames = new ArrayList<String>();
    for (File englishFile : englishFiles) {
      String baseFileName = chop(englishFile.getAbsolutePath(), "."+ENGLISH_EXTENSION);
      baseFileNames.add(baseFileName);
    }
    return baseFileNames;
  }

  private static String chop(String name, String extension) {
    if (! name.endsWith(extension)) return name;
    return name.substring(0, name.length()-extension.length());
  }

}
