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
  
  public static class Triple {
	  int numEnglishWords;
	  int numFrenchWords;
	  int frenchPosition;
	  
	  public int position() {
		  return frenchPosition;
	  }
	  
	  public int L() {
		  return numEnglishWords;
	  }
	  
	  public int M() {
		  return numFrenchWords;
	  }
	  
	  public Triple (int i, int l, int m) {
		  frenchPosition = i;
		  numEnglishWords = l;
		  numFrenchWords = m;
	  }
	  
	  public int hashCode() {
			int result;
			result = numEnglishWords;
			result = 100 * result + numFrenchWords;
			result = 10000 * result + frenchPosition;
			return result;
	  }
	  
	  public boolean equals(Object obj) {
		  Triple ilm = (Triple) obj;
		  if (this.frenchPosition != ilm.frenchPosition)
			  return false;
		  if (this.numFrenchWords != ilm.numFrenchWords)
			  return false;
		  if (this.numEnglishWords != ilm.numEnglishWords)
			  return false;
		  return true;
	  }
  }
  
  public static class ParameterEstimate {
	  CounterMap<String, String> t;
	  CounterMap<Triple,Integer> q;
	  
	  public void updateT (String f, String e, double prob) {
		  t.setCount(f, e, prob);
	  }
	  
	  public void updateQ (int englishPosition, int frenchPosition, int numEnglishWords, int numFrenchWords, double prob) {
		  Triple ilm = new Triple(frenchPosition, numEnglishWords, numFrenchWords);
		  q.setCount(ilm, englishPosition, prob);
	  }
	  
	  public ParameterEstimate() {
		  t = new CounterMap<String,String>();
		  q = new CounterMap<Triple,Integer>();
	  }
	  
	  public ParameterEstimate(CounterMap<String,String> translations, CounterMap<Triple,Integer> alignments) {
		  t = translations;
		  q = alignments;
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
		      //Set<String> frenchSet = new Set<String>(frenchWords);
		      //Set<String> englishSet = new Set<String>(englishWords);
		      
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

  static class UnionizedAligner extends IntersectedAligner implements WordAligner {
	  
	  public Alignment alignSentencePair (SentencePair sentencePair) {
		  Alignment alignmentLeft = left.alignSentencePair(sentencePair);
		  Alignment alignmentRight = right.alignSentencePair(super.flipSentence(sentencePair));
		  Alignment unionized = new Alignment();
		  
		  for (Pair<Integer,Integer> alignLeftPossible : alignmentLeft.possibleAlignments) {
			  int leftPosition = alignLeftPossible.getFirst();
			  int rightPosition = alignLeftPossible.getSecond();
			  
			  if (alignmentLeft.containsSureAlignment(leftPosition, rightPosition) &&
					  alignmentRight.containsSureAlignment(rightPosition, leftPosition))
				  unionized.addAlignment(leftPosition, rightPosition, true);
			  else 
				  unionized.addAlignment(leftPosition, rightPosition, false);
		  }
		  
		  for (Pair<Integer,Integer> alignRightPossible : alignmentRight.possibleAlignments) {
			  int leftPosition = alignRightPossible.getSecond();
			  int rightPosition = alignRightPossible.getFirst();
			  
			  if (!alignmentLeft.containsPossibleAlignment(leftPosition, rightPosition)) {
				  unionized.addAlignment(leftPosition, rightPosition, false);
			  }
		  }
		  return unionized;
	  }
	  
	  public UnionizedAligner(WordAligner originalAligner,
			  List<SentencePair> data, int maxIterations, boolean initializeEM, 
			  boolean useDistortionNormalized, double reservedNullProb, double alpha) {
		  super(originalAligner, data, maxIterations, initializeEM, 
				  useDistortionNormalized, reservedNullProb, alpha);
	  }
	  
	  public UnionizedAligner(WordAligner originalAligner, 
			  List<SentencePair> data, int maxIterations, double reservedNullProb) {
		  super(originalAligner, data, maxIterations, reservedNullProb);
	  }
  }
  
  static class IntersectedAligner implements WordAligner {
	  WordAligner left;
	  WordAligner right;
	  List<SentencePair> trainingSentencePairs; 
	  List<SentencePair> flippedSentencePairs;
	  
	  private void createFlipped() {
		  for (SentencePair sentencePair : trainingSentencePairs) {
			  SentencePair flipped = flipSentence(sentencePair);
			  flippedSentencePairs.add(flipped);
		  }
	  }
	  
	  private static SentencePair flipSentence (SentencePair sentencePair) {
		  SentencePair flipped = new SentencePair(sentencePair.getSentenceID(), sentencePair.getSourceFile(),
				  sentencePair.getFrenchWords(), sentencePair.getEnglishWords());
		  return flipped;
	  }
	  
	  public Alignment alignSentencePair (SentencePair sentencePair) {
		  Alignment alignmentLeft = left.alignSentencePair(sentencePair);
		  Alignment alignmentRight = right.alignSentencePair(flipSentence(sentencePair));
		  Alignment intersected = new Alignment();
		  
		  for (Pair<Integer,Integer> alignLeftPossible : alignmentLeft.possibleAlignments) {
			  int leftPosition = alignLeftPossible.getFirst();
			  int rightPosition = alignLeftPossible.getSecond();
			  // if both alignments exist and both sure, add sure alignment
			  // if both alignments exist and one or both possible, add possible alignment
			  if (alignmentLeft.containsSureAlignment(leftPosition, rightPosition) &&
					  alignmentRight.containsSureAlignment(rightPosition, leftPosition))
				  intersected.addAlignment(leftPosition, rightPosition, true);
			  else if (alignmentRight.containsPossibleAlignment(rightPosition, leftPosition))
				  intersected.addAlignment(leftPosition, rightPosition, false);
		  }
		  return intersected;
	  }
	  
	  public IntersectedAligner(WordAligner originalAligner, 
			  List<SentencePair> data, int maxIterations, double reservedNullProb) {
		  this.left = (IBMmodel1WordAligner) originalAligner;
		  this.trainingSentencePairs = data;
		  this.flippedSentencePairs = new ArrayList<SentencePair>();
		  createFlipped();
		  this.right = new IBMmodel1WordAligner(flippedSentencePairs, maxIterations, reservedNullProb);
	  }
	  
	  public IntersectedAligner(WordAligner originalAligner,
			  List<SentencePair> data, int maxIterations, 
			  boolean initializeEM, boolean useDistortionNormalized, 
			  double reservedNullProb, double alpha) {
		  this.left = (IBMmodel2WordAligner) originalAligner;
		  this.trainingSentencePairs = data;
		  this.flippedSentencePairs = new ArrayList<SentencePair>();
		  createFlipped();
		  this.right = new IBMmodel2WordAligner(flippedSentencePairs, maxIterations, initializeEM, 
				  useDistortionNormalized, reservedNullProb, alpha);
	  }
  }
  
  static class IBMmodel1WordAligner implements WordAligner {
	  List<SentencePair> trainingSentencePairs; 
	  CounterMap<String,String> translationProb; 
	  static final String NULL = "<NULL>"; 
	  static final int nullPosition = -1;
	  static final double minimumThreshold = 1e-8;
	  double reservedNullProb;
	  
	  public Alignment alignSentencePair (SentencePair sentencePair) {
		  Alignment alignment = new Alignment();
		  List<String> frenchWords = sentencePair.getFrenchWords();
	      List<String> englishWords = sentencePair.getEnglishWords();     
	      int numFrenchWords = frenchWords.size();
	      int numEnglishWords = englishWords.size();
	      Counter<Integer> distortions = getAlignments(numEnglishWords);
	      
		  // Model 1 assumes all alignments are equally likely (EXCEPT NULL, which is special)
	      // So we can just take the argMax of t(f|e) to get the englishMaxPosition
	      // q(j|i.l.m) is constant for all j>NULL so can be (almost) ignored
	      for (int frenchPosition = 0; frenchPosition < numFrenchWords; frenchPosition++) {
	    	  String f = frenchWords.get(frenchPosition);
	    	  // Find argMax over all englishPosition j from 0 to numEnglishWords
	    	  int englishMaxPosition = nullPosition;
	    	  double maxProbability = translationProb.getCount(f, NULL) * distortions.getCount(nullPosition);
	    	  for (int englishPosition = 0; englishPosition < numEnglishWords; englishPosition++) {
	    		  String e = englishWords.get(englishPosition);
	    		  double probability = translationProb.getCount(f, e) * distortions.getCount(englishPosition);
	    		  if (probability > maxProbability) {
	    			  englishMaxPosition = englishPosition;
	    			  maxProbability = probability;
	    		  }
	    	  }
	    	  alignment.addAlignment(englishMaxPosition, frenchPosition, true);
	      }
		  
		  return alignment;
	  }
	  
	  private CounterMap<String,String> trainEM (int maxIterations) {
		  CounterMap<String,String> translations = new CounterMap<String,String>();
		  Counter<Integer> alignments = new Counter<Integer>();
		  Set<String> frenchVocab = new HashSet<String>();
		  Set<Pair<String,String>> initializedT = new HashSet<Pair<String,String>>();
		  
		  // initialize the parameter estimates
		  for (SentencePair sentencePair : trainingSentencePairs) {
			  List<String> frenchWords = sentencePair.getFrenchWords();
			  // add words from list to vocabulary sets
			  frenchVocab.addAll(frenchWords);
		  }
		  // We need to initialize translationProb.getCount(f,e) uniformly
		  // for all e in {E + null} : t(f|e) summed over all f in {F} = 1
		  double initialCount = 1.0 / frenchVocab.size();
		  initialCount = Math.max(initialCount, minimumThreshold);
		  
		  for (int s=0; s < maxIterations; s++) {
			  
			  // Set all counts to 0
			  CounterMap<String,String> counts = new CounterMap<String,String>(); // set count(f|e) to 0 for all e,f
			  Counter<String> totalEnglish = new Counter<String>(); // set total(e) to 0 for all e
			  
			  // Iterate through all training sentences
			  // Estimate expected counts from current parameter estimates (E-step)
			  for (SentencePair sentencePair : trainingSentencePairs) {
				  List<String> frenchWords = sentencePair.getFrenchWords();
				  List<String> englishWords = sentencePair.getEnglishWords();		  
			      int numFrenchWords = frenchWords.size();
			      int numEnglishWords = englishWords.size();
			      Counter<String> normalizationConstant = new Counter<String>();
			      alignments = getAlignments(numEnglishWords);
			      
			      for (int frenchPosition=0; frenchPosition < numFrenchWords; frenchPosition++) {
			    	  String f = frenchWords.get(frenchPosition);
			    	  String e = NULL;
			    	  
			    	  for (int englishPosition=nullPosition; englishPosition < numEnglishWords; englishPosition++) {
			    		  if (englishPosition > nullPosition) {
			    			  e = englishWords.get(englishPosition);
			    		  }
			    		  // check if t(f|e) has been initialized, otherwise set to uniform
			    		  Pair<String,String> fe = Pair.makePair(f,e);
			    		  if (!translations.getCounter(f).containsKey(e)) {
			    			  if (!initializedT.contains(fe)) {
			    				  translations.setCount(f, e, initialCount);
			    				  initializedT.add(fe);
			    			  }
			    		  }
			    		  // compute normalization constant over all English words, including NULL
						  double tq = translations.getCount(f, e) * alignments.getCount(englishPosition);
			    		  normalizationConstant.incrementCount(f, tq);  
			    	  }
			      }
			      
			      // Now we have delta(k, i, j) = translations.getCount(f, e) * alignments.getCount(j) 
			      // normalizationConstant.getCount(f) 	for all f, e in training sentencePair k
			      
			      for (int frenchPosition=0; frenchPosition < numFrenchWords; frenchPosition++) {
			    	  String f = frenchWords.get(frenchPosition);
			    	  String e = NULL;
			    	  double delta, tq;
			    	  for (int englishPosition=nullPosition; englishPosition < numEnglishWords; englishPosition++) {
			    		  if (englishPosition > nullPosition) {
			    			  e = englishWords.get(englishPosition);
			    		  }
			    		  tq = translations.getCount(f, e) * alignments.getCount(englishPosition);
			    		  delta = tq  / normalizationConstant.getCount(f);
			    		  
			    		  // increment expected counts using delta
			    		  counts.incrementCount(e, f, delta);
			    		  totalEnglish.incrementCount(e, delta);
			    	  }
			      }
			  } // All sentences trained, end of count estimation (E-step)
			  System.out.println("iteration: "+(s+1)+ " end of E-step");
			  
			  // Update parameter estimates from estimated expected counts (M-step)
			  CounterMap<String,String> t = new CounterMap<String,String>();
			  for (String e: counts.keySet()) {
				  double normalizer = totalEnglish.getCount(e);
				  for (String f: (counts.getCounter(e)).keySet()) {
					  double expectedCount = counts.getCount(e, f);
					  if ((expectedCount / normalizer) > minimumThreshold)
						  t.setCount(f, e, expectedCount / normalizer);
				  }
			  }
			  translations = t;
			  System.out.println("iteration: "+(s+1)+ " end of M-step");
		  }
		  return translations;
	  }
	  
	  private Counter<Integer> getAlignments (int numEnglishWords) {
		  Counter<Integer> alignments = new Counter<Integer>();
		  for (int englishPosition=0; englishPosition < numEnglishWords; englishPosition++) {
			  alignments.setCount(englishPosition, 1.0);
		  }
		  alignments.normalize();
		  alignments.scale(1.0 - reservedNullProb);
		  alignments.setCount(nullPosition, reservedNullProb);
		  return alignments;
	  }
	  
	  public IBMmodel1WordAligner(List<SentencePair> data, int maxIterations, double reservedNull) {
		  this.trainingSentencePairs = data;
		  this.reservedNullProb = reservedNull;
		  translationProb = this.trainEM(maxIterations);
	  }
  }
  
  static class IBM2BucketsWordAligner implements WordAligner {
	 
	  public Alignment alignSentencePair (SentencePair sentencePair) {
		  Alignment alignment = new Alignment();
		  return alignment;
	  }
	  
	  public IBM2BucketsWordAligner (List<SentencePair> data, int maxIterations, 
			  int numBuckets) {
		  
	  }
  }
  
  static class IBMmodel2WordAligner  implements WordAligner {
	  List<SentencePair> trainingSentencePairs; 
	  CounterMap<String,String> translationProb;
	  CounterMap<Triple,Integer> alignmentProb;
	  boolean initializeEM;
	  boolean useDistortionNormalized = true;
	  static final String NULL = "<NULL>";
	  static final int nullPosition = -1; 
	  static final double minimumThreshold = 1e-8;
	  
	  // hyperparameters of the model, ideally we would like to learn these values from data
	  double alpha; // this controls how strong the preference for diagonal is in the exponential model
	  double reservedNullProb; // this controls the fixed probability of q(NULL|i,l,m) for all Triple(i,l,m)
	  
	  public Alignment alignSentencePair (SentencePair sentencePair) {
		  Alignment alignment = new Alignment();
		  List<String> frenchWords = sentencePair.getFrenchWords();
	      List<String> englishWords = sentencePair.getEnglishWords();     
	      int numFrenchWords = frenchWords.size();
	      int numEnglishWords = englishWords.size();
	      Counter<Integer> distortions = new Counter<Integer>();
	      
		  // Model 1 assumes all alignments are equally likely (EXCEPT NULL, which is special)
	      // So we can just take the argMax of t(f|e) to get the englishMaxPosition
	      // q(j|i.l.m) is constant for all j>NULL so can be (almost) ignored
	      for (int frenchPosition = 0; frenchPosition < numFrenchWords; frenchPosition++) {
	    	  String f = frenchWords.get(frenchPosition);
	    	  Triple ilm = new Triple (frenchPosition, numEnglishWords, numFrenchWords);
	    	  distortions = getInitialQ(ilm, alpha);
	    	  // Find argMax over all englishPosition j from 0 to numEnglishWords
	    	  int englishMaxPosition = nullPosition;
	    	  double maxProbability = translationProb.getCount(f, NULL) * distortions.getCount(nullPosition);
	    	  for (int englishPosition = 0; englishPosition < numEnglishWords; englishPosition++) {
	    		  String e = englishWords.get(englishPosition);
	    		  double probability = translationProb.getCount(f, e) * distortions.getCount(englishPosition);
	    		  if (probability > maxProbability) {
	    			  englishMaxPosition = englishPosition;
	    			  maxProbability = probability;
	    		  }
	    	  }
	    	  alignment.addAlignment(englishMaxPosition, frenchPosition, true);
	      }
		  
		  return alignment;
	  }
	  
	  private ParameterEstimate trainEM (int maxIterations) {
		  CounterMap<String,String> translations = new CounterMap<String,String>();
		  CounterMap<Triple,Integer> distortedAlignments = new CounterMap<Triple,Integer>();
		  Counter<Integer> alignments = new Counter<Integer>();
		  Set<String> frenchVocab = new HashSet<String>();
		  Set<Pair<String,String>> initializedT = new HashSet<Pair<String,String>>();
		  Set<Triple> initializedQ = new HashSet<Triple>();
		  
		  // initialize the parameter estimates
		  for (SentencePair sentencePair : trainingSentencePairs) {
			  List<String> frenchWords = sentencePair.getFrenchWords();
			  // add words from list to vocabulary sets
			  frenchVocab.addAll(frenchWords);
		  }
		  // We need to initialize translationProb.getCount(f,e) uniformly
		  // for all e in {E + null} : t(f|e) summed over all f in {F} = 1
		  double initialCount = 1.0 / frenchVocab.size();
		  initialCount = Math.max(initialCount, minimumThreshold);
		  
		  CounterMap<String,String> initialTranslations = new CounterMap<String,String>();
		  if (initializeEM) {
			  IBMmodel1WordAligner initialAligner = new IBMmodel1WordAligner(trainingSentencePairs, maxIterations, 
					  reservedNullProb);
			  initialTranslations = initialAligner.translationProb;
		  }
			  
		  for (int s=0; s < maxIterations; s++) {
			  
			  // Set all counts to 0
			  CounterMap<String,String> counts = new CounterMap<String,String>(); // set count(f|e) to 0 for all e,f
			  Counter<String> totalEnglish = new Counter<String>(); // set total(e) to 0 for all e
			  
			  // Iterate through all training sentences
			  // Estimate expected counts from current parameter estimates (E-step)
			  for (SentencePair sentencePair : trainingSentencePairs) {
				  List<String> frenchWords = sentencePair.getFrenchWords();
				  List<String> englishWords = sentencePair.getEnglishWords();		  
			      int numFrenchWords = frenchWords.size();
			      int numEnglishWords = englishWords.size();
			      Counter<String> normalizationConstant = new Counter<String>();
			      
			      
			      for (int frenchPosition=0; frenchPosition < numFrenchWords; frenchPosition++) {
			    	  String f = frenchWords.get(frenchPosition);
			    	  String e = NULL;
			    	  Triple ilm = new Triple(frenchPosition, numEnglishWords, numFrenchWords);
			    	  
			    	  if (!initializedQ.contains(ilm)) {
			    		  alignments = getInitialQ(ilm, alpha);
			    		  for (int position : alignments.keySet()) {
				    		  distortedAlignments.setCount(ilm, position, alignments.getCount(position));
				    	  }
			    		  initializedQ.add(ilm);
			    	  }
			    	  alignments = distortedAlignments.getCounter(ilm);
			    	  
			    	  for (int englishPosition=nullPosition; englishPosition < numEnglishWords; englishPosition++) {
			    		  if (englishPosition > nullPosition) {
			    			  e = englishWords.get(englishPosition);
			    		  }
			    		  // check if t(f|e) has been initialized, otherwise set to uniform
			    		  Pair<String,String> fe = Pair.makePair(f,e);
			    		  if (!translations.getCounter(f).containsKey(e)) {
			    			  if (!initializedT.contains(fe)) {
			    				  if (initializeEM)
			    					  translations.setCount(f, e, Math.max(initialTranslations.getCount(f,e), minimumThreshold));
			    				  else
			    					  translations.setCount(f, e, initialCount);
			    				  initializedT.add(fe);
			    			  }
			    			  else {
			    				  translations.setCount(f, e, minimumThreshold);
			    			  }
			    		  }
			    		  // compute normalization constant over all English words, including NULL
						  double tq = translations.getCount(f, e) * alignments.getCount(englishPosition);
			    		  normalizationConstant.incrementCount(f, tq);  
			    	  }
			      }
			      
			      // Now we have delta(k, i, j) = translations.getCount(f, e) * alignments.getCount(j) 
			      // normalizationConstant.getCount(f) 	for all f, e in training sentencePair k
			      
			      for (int frenchPosition=0; frenchPosition < numFrenchWords; frenchPosition++) {
			    	  String f = frenchWords.get(frenchPosition);
			    	  String e = NULL;
			    	  Triple ilm = new Triple(frenchPosition, numEnglishWords, numFrenchWords);
			    	  alignments = distortedAlignments.getCounter(ilm);
			    	  
			    	  double delta, tq;
			    	  for (int englishPosition=nullPosition; englishPosition < numEnglishWords; englishPosition++) {
			    		  if (englishPosition > nullPosition) {
			    			  e = englishWords.get(englishPosition);
			    		  }
			    		  tq = translations.getCount(f, e) * alignments.getCount(englishPosition);
			    		  delta = tq  / normalizationConstant.getCount(f);
			    		  
			    		  // increment expected counts using delta
			    		  counts.incrementCount(e, f, delta);
			    		  totalEnglish.incrementCount(e, delta);
			    	  }
			      }
			  } // All sentences trained, end of count estimation (E-step)
			  System.out.println("iteration: "+(s+1)+ " end of E-step");
			  
			  // Update parameter estimates from estimated expected counts (M-step)
			  CounterMap<String,String> t = new CounterMap<String,String>();
			  for (String e: counts.keySet()) {
				  double normalizer = totalEnglish.getCount(e);
				  for (String f: (counts.getCounter(e)).keySet()) {
					  double expectedCount = counts.getCount(e, f);
					  if ((expectedCount / normalizer) > minimumThreshold)
						  t.setCount(f, e, expectedCount / normalizer);
				  }
			  }
			  translations = t;
			  System.out.println("iteration: "+(s+1)+ " end of M-step");
		  }
		  ParameterEstimate parameters = new ParameterEstimate(translations, distortedAlignments);
		  //return translations;
		  return parameters;
	  }
	  
	  private Counter<Integer> getInitialQ (Triple ilm) {
		  Counter<Integer> initialQ = new Counter<Integer>();
		  initialQ.setCount(nullPosition, reservedNullProb);
		  for (int j=0; j < ilm.L(); j++) {
			  initialQ.setCount(j, (1.0 - reservedNullProb) / ilm.L());
		  }
		  return initialQ;
	  }
	  
	  private Counter<Integer> getInitialQ (Triple ilm, double alpha) {
		  Counter<Integer> initialQ = new Counter<Integer>();
		  initialQ = computeNormalizedDistortion(ilm.position(), ilm.L(), ilm.M(), alpha);
		  reallocateNullMass(initialQ, reservedNullProb);
		  return initialQ;
	  }
	  
	  private Counter<Integer> computeNormalizedDistortion (int frenchPosition,
			  int numEnglishWords, int numFrenchWords, double alpha) {
		  Counter<Integer> distortions = new Counter<Integer>();
		  
		  for (int englishPosition=0; englishPosition < numEnglishWords; englishPosition++) {
			  double a = ((double) englishPosition) / numEnglishWords;
			  double b = ((double) frenchPosition) / numFrenchWords;
			  double h = Math.abs(a-b);
			  //double ratio = ((double) numEnglishWords) / numFrenchWords;
			  //double h = Math.abs((englishPosition) - (frenchPosition)*ratio); 
			  h = -1.0 * alpha * h;
			  distortions.setCount(englishPosition, Math.exp(h));
		  }
		  distortions.normalize();
		  return distortions;
	  }
	  
	  private void reallocateNullMass (Counter<Integer> probability, double kNullProb, int nullPosition) {
		  probability.scale(1.0 - kNullProb);
		  probability.setCount(nullPosition, kNullProb);
	  }
	  
	  private void reallocateNullMass (Counter<Integer> probability, double kNullProb) {
		  reallocateNullMass(probability, kNullProb, -1);
	  }
	  
	  public IBMmodel2WordAligner(List<SentencePair> data, int maxIterations, 
			  boolean initializeEM, boolean useDistortionNormalized, 
			  double reservedNullProb, double alpha) {
		  this.reservedNullProb = reservedNullProb;
		  this.alpha = alpha;
		  
		  this.trainingSentencePairs = data;
		  this.initializeEM = initializeEM; // if true, train t(f|e) with IBM model 1 for maxIterations/2
		  this.useDistortionNormalized = useDistortionNormalized; // this is always true, for now
		  ParameterEstimate parameters = trainEM(maxIterations);
		  this.translationProb = parameters.t;
		  this.alignmentProb = parameters.q;
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
    boolean initializeEM = false;
    boolean distortion = true;
    String dataset = "mini";
    String model = "baseline";
    double reservedNull = 0.1;
    double alpha = 1.0;
    boolean intersect = false;
    boolean union = false;

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
    if (argMap.containsKey("-null")) {
    	reservedNull = Double.parseDouble(argMap.get("-null"));
    }
    if (argMap.containsKey("-intersect")) {
    	intersect = true;
    	System.out.println("Intersecting French to English with English to French");
    }
    if (argMap.containsKey("-union")) {
    	union = true;
    	intersect = false;
    	System.out.println("Taking the union of French to English with English to French");
    }
    
    if (argMap.containsKey("-verbose")) {
      verbose = true;
    }
    if (argMap.containsKey("-iterations")) {
    	maxIterations = Integer.parseInt(argMap.get("-iterations"));
    }
    if (argMap.containsKey("-initialize")) {
    	initializeEM = true;
    }
    if (argMap.containsKey("-alpha")) {
    	alpha = Double.parseDouble(argMap.get("-alpha"));
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
    	wordAligner = new IBMmodel1WordAligner(trainingSentencePairs, maxIterations, reservedNull);
    	if (intersect) {
    		wordAligner = new IntersectedAligner(wordAligner, 
    				trainingSentencePairs, maxIterations, reservedNull);
    	}
    	else if (union) {
    		wordAligner = new UnionizedAligner(wordAligner,
    				trainingSentencePairs, maxIterations, reservedNull);
    	}
    }
    else if (model.equalsIgnoreCase("ibm2") || model.equalsIgnoreCase("ibmModel2")) {
    	wordAligner = new IBMmodel2WordAligner(trainingSentencePairs, maxIterations, initializeEM,
    			distortion, reservedNull, alpha);
    	if (intersect) {
    		wordAligner = new IntersectedAligner(wordAligner, trainingSentencePairs,
    				maxIterations, initializeEM, distortion, reservedNull, alpha);
    	}
    	else if (union) {
    		wordAligner = new UnionizedAligner(wordAligner, trainingSentencePairs,
    				maxIterations, initializeEM, distortion, reservedNull, alpha);
    	}
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
