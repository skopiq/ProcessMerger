/*
 * Copyright © 2009-2014 The Apromore Initiative.
 *
 * This file is part of “Apromore”.
 *
 * “Apromore” is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * “Apromore” is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/lgpl-3.0.html>.
 */

package test;

import java.util.HashSet;
import java.util.LinkedList;

import common.EPCModelParser;
import common.IdGeneratorHelper;
import common.Settings;
import digest.Digest;

import graph.Graph;
import merge.MergingPaper;

public class MergeProcessModelsPaper {
	
	private static int digest = 0;
	private static boolean digestNeeded = false;
	private static String mergedName = null;
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		if (args.length == 0 || (args.length == 1 && "-help".equals(args[0]))){
			printHelp();
		} else if (args.length == 2 && args[0].startsWith("-digest")) {
			String param = args[0];
			int separator = param.indexOf("=");
			if (separator == -1) {
				System.out.println("Invalid parameters. Parameters and values must be separated by = sign.");
				printHelp();
				return;
			}
			String value = param.substring(separator + 1);
			try {
				digest = Integer.parseInt(value);
				if (digest < 0) {
					throw new Exception();
				}
				digestNeeded = true;
			} catch (Exception e) {
				System.out.println("Invalid parameter value for digest - "+ value +".\nDigest parameter value must be a positive integer.");
				printHelp();
				return;
			}
			testDigest(args[1]);
			
		} else {
			HashSet<String> containedParams = new HashSet<String>();
			LinkedList<String> modelnames = new LinkedList<String>();
			for (int i = 0; i < args.length; i++) {
				String param = args[i];
				boolean paramEnd = false;
				// this is a parameter
				if (!paramEnd && param.startsWith("-")) {
					int separator = param.indexOf("=");
					if (separator == -1) {
						System.out.println("Invalid parameters. Parameters and values must be separated by = sign.");
						printHelp();
						return;
					}
					String paramHead = param.substring(1, separator);
					String value = param.substring(separator + 1);
					if (containedParams.contains(paramHead)) {
						System.out.println("Parameter "+paramHead+ " occurred twice.");
						printHelp();
						return;
					} else {
						processParameters(paramHead, value);
						containedParams.add(paramHead);
					}
				} else {
					paramEnd = true;
					// other ones should me model names
					modelnames.add(param);
				}
			}
			
			// parameters processed
			if (modelnames.size() < 2) {
				System.out.println("Give at least 2 modelnames.");
				printHelp();
				return;
			}
			testMerge(modelnames);
		}
	}
	
	private static void processParameters(String parameter, String value) {
		
		if ("mt".equals(parameter) || "ct".equals(parameter)) {
			Double v = 0.0;
			try {
				v = Double.parseDouble(value);
				if (v < 0 || v > 1) {
					throw new Exception();
				}
			} catch (Exception e) {
				System.out.println("Invalid parameter value for threshold - "+ value +".\nParameter value for merge and context treshold must be double number in range [0, 1].");
				printHelp();
				System.exit(0);
			}
			// now the parameter should be also ok
			if ("mt".equals(parameter)) {
				Settings.MERGE_THRESHOLD = v;
			} else {
				Settings.MERGE_CONTEXT_THRESHOLD = v;
			}
		} else if ("ent".equals(parameter)) {
			if ("true".equals(value)) {
				Settings.REMOVE_ENTANGLEMENT = true;
			} else if ("false".equals(value)) {
				Settings.REMOVE_ENTANGLEMENT = false;
			} else {
				System.out.println("Invalid parameter value for entanglement - "+ value+".\nEntanglement parameter value can be only true or false.");
				printHelp();
				System.exit(0);
			}
		} else if ("digest".equals(parameter)) {
			try {
				digest = Integer.parseInt(value);
				if (digest < 0) {
					throw new Exception();
				}
				digestNeeded = true;
			} catch (Exception e) {
				System.out.println("Invalid parameter value for digest - "+ value +".\nDigest parameter value must be a positive integer.");
				printHelp();
				System.exit(0);
			}
		} else if ("alg".equals(parameter)) {
			if ("h".equals(value)) {
				Settings.COMPARISON_METHOD = Settings.ComparisonMethod.Hungarian;
			} else if ("g".equals(value)) {
				Settings.COMPARISON_METHOD = Settings.ComparisonMethod.Greedy;
			} else {
				System.out.println("Invalid parameter value for agorithm - "+ value +".\nAlgorithm parameter should be h for hungarian and g for greedy.");
				printHelp();
				System.exit(0);
			}
		} else if ("name".equals(parameter)) {
			mergedName = value;
		} else {
			System.out.println("Unknown parameter - "+ parameter);
			printHelp();
			System.exit(0);
		}
	}

	private static void printHelp() {
		System.out.println("USAGE1: \n" +
				"ProcessMerger [-mt=mt_value] [-ct=ct_value] [-ent=[true|false]] " +
				"[-digest=diges_value] [-alg=[g|h]] [-name=merged_name] model1 model2 ... modeln\n" +
				"-mt - threshold for non connector nodes, if similarity for nodes is >= threshold, " +
				"then 2 nodes are merged, default value=0.5\n" + 
				"-ct - threshold for connector nodes, if similarity for connectors is >= threshold, " +
				"then 2 connectors are merged, default value=0.75\n" + 
				"-ent - defines if mapped pairs that entangle should be removed or not, \n" +
				"if true, then entanglement pairs should be removed, default value=true\n" +
				"-digest - defines if the digest of the merged model should be calculated, \n" +
				"needs parameter frequency n - all the nodes that occur at least n times will be added to digest \n"+
				"-alg - algorithm that is used for similarity calculation for merge,\n h - hungarian, g - greedy, default is greedy.\n" +
				"-name - name for the merged model, by default the concatenation of merged model names are used.\n\n" +
				"USAGE2: \n" +
				"ProcessMerger [-digest=diges_value] mergedModel\n" +
				"-digest - defines that the digest of the merged model should be calculated, \n" +
				"needs parameter frequency n - all the nodes that occur at least n times will be added to digest.\n" +
				"\nNote that the name parameter for all models must be different."
				);
	}

	private static void testMerge(LinkedList<String> modelnames) {

        IdGeneratorHelper idGenerator = new IdGeneratorHelper();

		Graph g1 = EPCModelParser.readModels(modelnames.get(0), false, idGenerator).get(0);
		g1.setIdGenerator(idGenerator);
		g1.removeEmptyNodes();

		g1.reorganizeIDs();
		
		Graph g2 = EPCModelParser.readModels(modelnames.get(1), false, idGenerator).get(0);
        g2.setIdGenerator(idGenerator);
		g2.removeEmptyNodes();
		g2.reorganizeIDs();
		
		g1.addLabelsToUnNamedEdges();
		g2.addLabelsToUnNamedEdges();

		Graph merged = new MergingPaper().mergeModels(g1, g2, idGenerator);
		
		if (modelnames.size() > 2) {
			for (int i = 2; i < modelnames.size(); i++) {
				Graph g3 = EPCModelParser.readModels(modelnames.get(i), false, idGenerator).get(0);
				g3.removeEmptyNodes();
				g3.reorganizeIDs();
				g3.addLabelsToUnNamedEdges();
				
				merged = new MergingPaper().mergeModels(merged, g3, idGenerator);
			}
		}
		EPCModelParser.writeModel(mergedName == null ? (merged.name + "_merged.epml") : mergedName+".epml", merged);	
		
		if (digestNeeded) {
			findDigest(merged, idGenerator);
		}
	}
	
	private static void testDigest(String model) {

        IdGeneratorHelper idGeneratorHelper = new IdGeneratorHelper();
		Graph g1 = EPCModelParser.readModels(model, false, idGeneratorHelper).get(0);
        g1.setIdGenerator(idGeneratorHelper);
		g1.reorganizeIDs();
		g1.addLabelsToUnNamedEdges();
		
		findDigest(g1, idGeneratorHelper);
	}
	
	private static void findDigest(Graph g, IdGeneratorHelper idGenerator) {
		Graph digest2 = Digest.digest(g, digest, idGenerator);
		EPCModelParser.writeModel(digest2.name+".epml", digest2);	
	}

}
