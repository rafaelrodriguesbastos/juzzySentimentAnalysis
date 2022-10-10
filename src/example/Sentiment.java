/*
 * Sentiment.java
 *
 * Created on Oct 07th 2022
 *
 * Based on Juzzy by Christian Wagner
 */
package example;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.tartarus.snowball.ext.PorterStemmer;

import generic.Input;
import generic.Output;
import generic.Tuple;
import tools.JMathPlotter;
import type1.sets.T1MF_Gauangle;
import type1.sets.T1MF_Gaussian;
import type1.sets.T1MF_Interface;
import type1.sets.T1MF_Trapezoidal;
import type1.sets.T1MF_Triangular;
import type1.system.T1_Antecedent;
import type1.system.T1_Consequent;
import type1.system.T1_Rule;
import type1.system.T1_Rulebase;

/**
 * A simple example of a type-1 FLS based on the "Sentiment Analysis".
 * 
 * @author Rafael Bastos
 */
public class Sentiment {
	Input negativity, positivity; // the inputs to the FLS
	Output classification; // the output of the FLS
	T1_Rulebase rulebase; // the rulebase captures the entire FLS

	public Sentiment() throws IOException {

		// Define the inputs
		negativity = new Input("Negativity degree", new Tuple(0, 1));
		positivity = new Input("Positivy degree", new Tuple(0, 1));
		classification = new Output("Tweet classification", new Tuple(0, 1));

		// Set up the membership functions (MFs) for each input and output
		double lowNegLimits[] = { 0.0, 0.0, 0.3, 0.5 };
		double moderateNegLimits[] = { 0.3, 0.45, 0.55, 0.7 };
		double highNegLimits[] = { 0.5, 0.7, 1.0, 1.0 };
		T1MF_Trapezoidal lowNegativityMF = new T1MF_Trapezoidal("MF for low negativity", lowNegLimits);
		T1MF_Trapezoidal moderateNegativityMF = new T1MF_Trapezoidal("MF for moderate negativity", moderateNegLimits);
		T1MF_Trapezoidal highNegativityMF = new T1MF_Trapezoidal("MF for high negativity", highNegLimits);

		T1MF_Triangular lowPositivityMF = new T1MF_Triangular("MF for low positivity", 0.0, 0.0, 0.5);
		T1MF_Triangular moderatePositivityMF = new T1MF_Triangular("MF for moderate positivity", 0.3, 0.5, 0.7);
		T1MF_Triangular highPositivityMF = new T1MF_Triangular("MF for high positivity", 0.5, 1.0, 1.0);

		double negativeLimits[] = { 0.0, 0.0, 0.3, 0.5 };
		double moderateneutralLimits[] = { 0.3, 0.45, 0.55, 0.7 };
		double positiveLimits[] = { 0.5, 0.7, 1.0, 1.0 };
		T1MF_Trapezoidal negativeClassificationMF = new T1MF_Trapezoidal("Negative classification", negativeLimits);
		T1MF_Trapezoidal neutralClassificationMF = new T1MF_Trapezoidal("Neutral classification",
				moderateneutralLimits);
		T1MF_Trapezoidal positiveClassificationMF = new T1MF_Trapezoidal("Positive classification", positiveLimits);

		// Set up the antecedents and consequents - note how the inputs are associated...
		T1_Antecedent lowNegativity = new T1_Antecedent("Low Negativity", lowNegativityMF, negativity);
		T1_Antecedent moderateNegativity = new T1_Antecedent("Moderate Negativity", moderateNegativityMF, negativity);
		T1_Antecedent highNegativity = new T1_Antecedent("High Negativity", highNegativityMF, negativity);

		T1_Antecedent lowPositivity = new T1_Antecedent("Low Positivity", lowPositivityMF, positivity);
		T1_Antecedent moderatePositivity = new T1_Antecedent("Moderate Positivity", moderatePositivityMF, positivity);
		T1_Antecedent highPositivity = new T1_Antecedent("High Positivity", highPositivityMF, positivity);

		T1_Consequent negativeClassification = new T1_Consequent("Negative", negativeClassificationMF, classification);
		T1_Consequent neutralClassification = new T1_Consequent("Neutral", neutralClassificationMF, classification);
		T1_Consequent positiveClassification = new T1_Consequent("Positive", positiveClassificationMF, classification);

		// Set up the rulebase and add rules 
		rulebase = new T1_Rulebase(9);
		rulebase.addRule(new T1_Rule(new T1_Antecedent[] { lowNegativity, lowPositivity }, neutralClassification));
		rulebase.addRule(
				new T1_Rule(new T1_Antecedent[] { moderateNegativity, moderatePositivity }, neutralClassification));
		rulebase.addRule(new T1_Rule(new T1_Antecedent[] { highNegativity, highPositivity }, neutralClassification));
		rulebase.addRule(
				new T1_Rule(new T1_Antecedent[] { lowNegativity, moderatePositivity }, negativeClassification));
		rulebase.addRule(new T1_Rule(new T1_Antecedent[] { lowNegativity, highPositivity }, negativeClassification));
		rulebase.addRule(
				new T1_Rule(new T1_Antecedent[] { moderateNegativity, highPositivity }, negativeClassification));
		rulebase.addRule(
				new T1_Rule(new T1_Antecedent[] { moderateNegativity, lowPositivity }, positiveClassification));
		rulebase.addRule(
				new T1_Rule(new T1_Antecedent[] { highNegativity, moderatePositivity }, positiveClassification));
		rulebase.addRule(new T1_Rule(new T1_Antecedent[] { highNegativity, lowPositivity }, positiveClassification));

		// just an example of setting the discretisation level of an output - the usual
		// level is 100 classification.setDiscretisationLevel(50);

		// load dataset file
		File f = new File("data/tweets.csv");
		Scanner lineScan = new Scanner(f);
		String line = new String();
		String[] data = new String[6];
		String tweet = new String();
		String stemmedWord;

		// English stopwords from https://www.kaggle.com/datasets/rowhitswami/stopwords
		List<String> stopwords = Files.readAllLines(Paths.get("data/stopwords.txt"));
		List<String> list;
		Boolean negation;

		while (lineScan.hasNextLine()) {
			// read a lowercase line removing URL and mentions (@someone)
			line = lineScan.nextLine().toLowerCase().replaceAll("http.*?\\s", "").replaceAll("@.*?\\s", "");
			// verify if there is negation word in the line
			if (line.contains(" not ")) {
				negation = true;
			} else {
				negation = false;
			}
			// split the dataset line in words
			data = line.split("\",\"");
			// get only the column 5, the tweet, without quotation marks
			tweet = data[5].replace("\"", "");
			// remove all number and punctuation from tweet then split into words
			List<String> tokens = new ArrayList<>(
					Arrays.asList(tweet.replaceAll("[0-9]", "").replaceAll("\\p{Punct}", "").split(" ")));
			tokens.removeAll(stopwords);

			String[] processedTweet = new String[tokens.size()];
			processedTweet = tokens.toArray(processedTweet);

			// Porter Stemming
			for (int i = 0; i < processedTweet.length; i++) {
				PorterStemmer stemmer = new PorterStemmer();
				stemmer.setCurrent(processedTweet[i]); // set string you need to stem
				stemmer.stem(); // stem the word
				stemmedWord = stemmer.getCurrent();// get the stemmed word
				//System.out.println(stemmedWord);
				
				//detect the language //case not english -> translate
				//verify negation
				//keep only opinion words: verbs, adjectives and adverbs
				
				

			}

		}
		lineScan.close();

		// get some outputs
		getClassification(0.6, 0.4);

		// plot some sets, discretizing each input into 100 steps.
		plotMFs("Negativity Membership Functions",
				new T1MF_Interface[] { lowNegativityMF, moderateNegativityMF, highNegativityMF },
				negativity.getDomain(), 100);
		plotMFs("Positivity Membership Functions",
				new T1MF_Interface[] { lowPositivityMF, moderatePositivityMF, highPositivityMF },
				positivity.getDomain(), 100);
		plotMFs("Classification Membership Functions",
				new T1MF_Interface[] { negativeClassificationMF, neutralClassificationMF, positiveClassificationMF },
				classification.getDomain(), 100);

		// plot control surface
		// do either height defuzzification (false) or centroid d. (true)
		plotControlSurface(true, 10, 10);

		// print out the rules
		System.out.println("\n" + rulebase);

	}

	/**
	 * Basic method that prints the output for a given set of inputs.
	 * 
	 * @param negativityMeasure
	 * @param positivityMeasure
	 * @throws IOException
	 */

	private void getClassification(double negativityMeasure, double positivityMeasure) {
		// first, set the inputs
		negativity.setInput(negativityMeasure);
		positivity.setInput(positivityMeasure);
		// now execute the FLS and print output
		System.out.println("The negativity measure was: " + negativity.getInput());
		System.out.println("The positivity measure was: " + positivity.getInput());
		System.out.println("Using height defuzzification " + rulebase.evaluate(0).get(classification));
		System.out.println("Using centroid defuzzification " + rulebase.evaluate(1).get(classification));
	}

	private void plotMFs(String name, T1MF_Interface[] sets, Tuple xAxisRange, int discretizationLevel) {
		JMathPlotter plotter = new JMathPlotter(17, 17, 15);
		for (int i = 0; i < sets.length; i++) {
			plotter.plotMF(sets[i].getName(), sets[i], discretizationLevel, xAxisRange, new Tuple(0.0, 1.0), false);
		}
		plotter.show(name);
	}

	private void plotControlSurface(boolean useCentroidDefuzzification, int input1Discs, int input2Discs) {
		double output;
		double[] x = new double[input1Discs];
		double[] y = new double[input2Discs];
		double[][] z = new double[y.length][x.length];
		double incrX, incrY;
		incrX = negativity.getDomain().getSize() / (input1Discs - 1.0);
		incrY = positivity.getDomain().getSize() / (input2Discs - 1.0);

		// first, get the values
		for (int currentX = 0; currentX < input1Discs; currentX++) {
			x[currentX] = currentX * incrX;
		}
		for (int currentY = 0; currentY < input2Discs; currentY++) {
			y[currentY] = currentY * incrY;
		}

		for (int currentX = 0; currentX < input1Discs; currentX++) {
			negativity.setInput(x[currentX]);
			for (int currentY = 0; currentY < input2Discs; currentY++) {
				positivity.setInput(y[currentY]);
				if (useCentroidDefuzzification)
					output = rulebase.evaluate(1).get(classification);
				else
					output = rulebase.evaluate(0).get(classification);
				z[currentY][currentX] = output;
			}
		}

		// now do the plotting
		JMathPlotter plotter = new JMathPlotter(17, 17, 14);
		plotter.plotControlSurface("Control Surface",
				new String[] { negativity.getName(), positivity.getName(), "Classification" }, x, y, z,
				new Tuple(0.0, 1.0), true);
		plotter.show("Type-1 Fuzzy Logic System Control Surface for Sentiment Analysis");
	}

	public static void main(String args[]) throws IOException {
		new Sentiment();
	}
}
