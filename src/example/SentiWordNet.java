package example;

/**
 * 
 */

/**
 * @author bastos
 *
 */
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SentiWordNet {

	private Map<String, Double[]> dictionary;

	public SentiWordNet(String pathToSWN) throws IOException {
		// This is our main dictionary representation
		dictionary = new HashMap<String, Double[]>();

		// From String to list of doubles.
		HashMap<String, HashMap<Integer, Double[]>> tempDictionary = new HashMap<String, HashMap<Integer, Double[]>>();

		BufferedReader csv = null;
		try {
			csv = new BufferedReader(new FileReader(pathToSWN));
			int lineNumber = 0;

			String line;
			while ((line = csv.readLine()) != null) {
				lineNumber++;

				// If it's a comment, skip this line.
				if (!line.trim().startsWith("#")) {
					// We use tab separation
					String[] data = line.split("\t");
					String wordTypeMarker = data[0]; //a, n, r, v

					// Example line:
					// POS ID PosS NegS SynsetTerm#sensenumber Desc
					// a 00009618 0.5 0.25 spartan#4 austere#3 ascetical#2 ascetic#2 practicing great self-denial;...etc

					// Is it a valid line? Otherwise, through exception.
					if (data.length != 6) {
						throw new IllegalArgumentException("Incorrect tabulation format in file, line: " + lineNumber);
					}

					// Calculate synset score as score = PosS - NegS
					Double[] synsetScores = {Double.parseDouble(data[2]), Double.parseDouble(data[3])};
					

					// Get all Synset terms
					String[] synTermsSplit = data[4].split(" ");

					// Go through all terms of current synset.
					for (String synTermSplit : synTermsSplit) {
						// Get synterm and synterm rank
						String[] synTermAndRank = synTermSplit.split("#");
						String synTerm = synTermAndRank[0] + "#" + wordTypeMarker;

						int synTermRank = Integer.parseInt(synTermAndRank[1]);
						// What we get here is a map of the type:
						// term -> {score of synset#1, score of synset#2...}

						// Add map to term if it doesn't have one
						if (!tempDictionary.containsKey(synTerm)) {
							tempDictionary.put(synTerm, new HashMap<Integer, Double[]>());
						}

						// Add synset link to synterm
						tempDictionary.get(synTerm).put(synTermRank, synsetScores);
						
					}
				}
			}

			// Go through all the terms.
			for (Map.Entry<String, HashMap<Integer, Double[]>> entry : tempDictionary.entrySet()) {
				String word = entry.getKey();
				Map<Integer, Double[]> synSetScoreMap = entry.getValue();

				// Calculate weighted average. Weigh the synsets according to
				// their rank.
				// Score= 1/2*first + 1/3*second + 1/4*third ..... etc.
				// Sum = 1/1 + 1/2 + 1/3 ...
				double scorePos = 0.0;
				double scoreNeg = 0.0;
				double sum = 0.0;
				for (Map.Entry<Integer, Double[]> entryScore : synSetScoreMap.entrySet()) {
					Integer rank = entryScore.getKey();
					Double[] synsetScores = entryScore.getValue();
					
					scorePos += synsetScores[0];
					scoreNeg += synsetScores[1];
					sum++; //= 1.0 / (double) entryScore.getKey();
				}
				Double[] scores = {scorePos/sum, scoreNeg/sum}; 
				dictionary.put(word, scores);
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (csv != null) {
				csv.close();
			}
		}
	}

	public Double[] score(String word, String pos) {
		String key = word + "#" + pos;
		if (dictionary.keySet().contains(key)) {
			Double[] scores = dictionary.get(key);

			return scores;
			
		} else {
			Double[] zeros = {0.0, 0.0};
			return zeros;
		}
//		double total = 0.0;
//	    if(dictionary.get(word+"#n") != null)
//	         total = dictionary.get(word+"#n") + total;
//	    if(dictionary.get(word+"#a") != null)
//	        total = dictionary.get(word+"#a") + total;
//	    if(dictionary.get(word+"#r") != null)
//	        total = dictionary.get(word+"#r") + total;
//	    if(dictionary.get(word+"#v") != null)
//	        total = dictionary.get(word+"#v") + total;
//	    return total;		
	}

}
