package edu.ucsd.sbrg.go;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import org.biojava3.core.sequence.ProteinSequence;
import org.biojava3.core.sequence.compound.AminoAcidCompound;
import org.biojava3.core.sequence.compound.AminoAcidCompoundSet;
import org.biojava3.core.sequence.io.FastaReader;
import org.biojava3.core.sequence.io.FastaWriter;
import org.biojava3.core.sequence.io.GenericFastaHeaderFormat;
import org.biojava3.core.sequence.io.PlainFastaHeaderParser;
import org.biojava3.core.sequence.io.ProteinSequenceCreator;
import org.biojava3.ontology.Ontology;
import org.biojava3.ontology.Term;
import org.biojava3.ontology.io.OboParser;

/*
 * Copyright (c) 2014 University of California, San Diego
 */

/**
 * @author James T Yurkovich
 * @since 1.0
 * @version $Rev$
 *
 */
public class Filter {

	/**
	 * This function will parse *.obo file.
	 *
	 * @param args
	 *            Input fasta file (TF/nonTF file), Output fasta file (order is mandatory)
	 * @throws Exception
	 *             This simple application will terminate whenever an error
	 *             occurs.
	 */
	public static void main(String[] args) throws Exception {
		// declare file variables
		
		//System.out.print(args[0]);
		
		File inputFile = new File (args[0]);
		
		if (!inputFile.exists() || !inputFile.canRead()) {
			System.err.println(inputFile.getAbsolutePath());
			System.err.println("Does exist: " + inputFile.exists());
			System.err.println("Can read: " + inputFile.canRead());
			System.err.println("Real error!");
		}

		Filter filter = new Filter();
		filter.filter(inputFile, new File (args[1]));

		//System.out.println(go.containsTerm("GO:0000001"));
		//System.out.println(isChildOf(go.getTerm("GO:0000023"), go.getTerm("GO:0005984")));
		//System.out.println(isChildOf(go.getTerm("GO:0000020"), go.getTerm("GO:0005984")));
	}

	/**
	 * Declaring ontology variable.
	 */
	private static Ontology go;

	// declaring our *.obo parser
	static {
		final OboParser parser = new OboParser();
		try {
			final InputStream is = Filter.class.getResourceAsStream("go.obo");
			go = parser.parseOBO(new BufferedReader(new InputStreamReader(is)), "GO", "Gene Ontology");
		} catch (final Throwable e) {
			e.printStackTrace();
			go = null;
		}
	}

	/**
	 * Traverses the gene ontology starting at {@link Term} subject
	 * until either the root (GO:0000000) or the {@link Term} object is
	 * reached.
	 *
	 * @param subject
	 *            Child
	 * @param object
	 *            Parent
	 * @return {@code true} if subject is a child of object.
	 */
	public static boolean isChildOf(Term subject, Term object) {
		if (subject.equals(object)) {
			return true;
		}
		final Set<org.biojava3.ontology.Triple> relations = go.getTriples(
				subject != null ? subject : null, null, null);
		for (final org.biojava3.ontology.Triple triple : relations) {
			if (triple.getObject().equals(object)) {
				return true;
			}
			if (isChildOf(triple.getObject(), object)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * logger
	 */
	private static final transient Logger logger = Logger.getLogger(Filter.class.getName());

	/**
	 *
	 * @param inputFile Input fasta file
	 * @param outputFile Output fasta file
	 * @throws Exception
	 */
	public void filter(File inputFile, File outputFile) throws Exception {

		final FastaReader<ProteinSequence,AminoAcidCompound> fastaReader =
				new FastaReader<ProteinSequence,AminoAcidCompound>(
						new FileInputStream(inputFile),
						new PlainFastaHeaderParser<ProteinSequence,AminoAcidCompound>(),
						new ProteinSequenceCreator(AminoAcidCompoundSet.getAminoAcidCompoundSet()));
		final LinkedHashMap<String, ProteinSequence> proteinSequenceHashMap = fastaReader.process();

		final List<Term> tfIdsList = new ArrayList<>();
		tfIdsList.add(go.getTerm("GO:0006355")); // name: regulation of transcription, DNA-templated
		tfIdsList.add(go.getTerm("GO:0001071")); // name: nucleic acid binding transcription factor activity

		/*tfIdsList.add(go.getTerm("GO:0032583")); // alt_id
		tfIdsList.add(go.getTerm("GO:0045449")); // alt_id
		tfIdsList.add(go.getTerm("GO:0061019")); // alt_id*/


		final List<ProteinSequence> listOfSequences = new ArrayList<>();

		int shouldNotBeTF = 0, shouldNotBeNonTF = 0, total = 0, TFs = 0, nonTFs = 0;
		for (final Entry<String, ProteinSequence> entry : proteinSequenceHashMap.entrySet()) {
			total++;
			final String header = entry.getKey();
			final ProteinSequence protSequence = entry.getValue();
			protSequence.setOriginalHeader(header.replace(' ', '_'));

			final StringTokenizer st = new StringTokenizer(header, "|");
			while (st.hasMoreElements()) {
				final String element = st.nextElement().toString();

				if (element.startsWith("GO:")) {
					final StringTokenizer gost = new StringTokenizer(element, ",");

					boolean correct = true;

					final boolean isLabeledNonTF = header.contains("nonTF");
					final boolean isLabeledTF = !isLabeledNonTF && header.contains("TF");
					boolean hasAtLeastOneGO_TFTerm = false;
					
					if (isLabeledNonTF) {
						nonTFs++;
					}
					else if (isLabeledTF) {
						TFs++;
					}

					while (gost.hasMoreElements()) {
						final Term currentTerm = go.getTerm(gost.nextElement().toString());
						for (final Term tfTerm : tfIdsList) {
							final boolean isTF = isChildOf(currentTerm, tfTerm);

							if (isLabeledNonTF && isTF) {
								correct = false;
								shouldNotBeNonTF++;
								break;
							}
							else if (isLabeledTF) {
								hasAtLeastOneGO_TFTerm |= isTF;
							}
						}
					}
					if (isLabeledTF && !hasAtLeastOneGO_TFTerm) {
						correct = false;
						shouldNotBeTF++;
					}

					if (correct) {
						listOfSequences.add(protSequence);
					} else {
						logger.warning("contradiction: " + header);
					}
				}
			}
		}

		final FastaWriter<ProteinSequence, AminoAcidCompound> fw  =
				new FastaWriter<ProteinSequence, AminoAcidCompound>(new FileOutputStream(outputFile),
						listOfSequences, new GenericFastaHeaderFormat<ProteinSequence, AminoAcidCompound>());

		fw.process();

		logger.info("total entries:\t" + proteinSequenceHashMap.size());
		logger.info("contradictions found:\t" + (shouldNotBeNonTF + shouldNotBeTF));
		logger.info("mislabeled nonTFs:\t" + shouldNotBeNonTF);
		logger.info("correct TFs:\t" + (TFs - shouldNotBeTF));
		logger.info("correct nonTFs:\t" + (nonTFs - shouldNotBeNonTF));
		logger.info("mislabeled TFs:\t" + shouldNotBeTF);
		logger.info("correctly labeled entries:\t" + (total - (shouldNotBeNonTF + shouldNotBeTF)));
	}

}
