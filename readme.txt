
  -----------------------------------------------------------------------------------
  TFpredict - Identification and structural characterization of transcription factors
  -----------------------------------------------------------------------------------
  (version 1.1, Copyright (C) 2012 Florian Topf and Johannes Eichner)


  Contents:
 _________________________
  - License
  - Installation
  - Manual
  - Format specification
  - Citation
  - Website and questions
____________________________________________________________________________________________________________________________  

  -------
  License
  -------

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 3 of the License, or (at
  your option) any later version.

  This program is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, see <http://www.gnu.org/licenses/>.

____________________________________________________________________________________________________________________________

  ------------
  Installation
  ------------

  To extract the gzipped tar archive of TFpredict obtained from our downloads section use the command:

    tar -xzf tf_predict.tar.gz

  TFpredict is completely implemented in Java and provided as a runnable JAR file. 
  All platforms (Windows, Mac, Linux) are supported provided that Java (JDK 1.6 or later) is installed. 
  The tool runs out of the box, i.e., it does not require any additional installation.

  ____________________________________________________________________________________________________________________________


  ------
  Manual
  ------

  DESCRIPTION:  TFpredict is a tool which implements a novel three-step classification method which expects 
  		a protein sequence as input and 
		(1) distinguishes transcription factors (TF) from other proteins (Non-TF), 
		(2) predicts the structural superclass of TFs (see TransFac classification), and 
		(3) identifies the DNA-binding domains of TFs. 
		Obviously, the latter two classification steps can only be performed if the given 
		protein sequence corresponds to a TF. For the TF/Non-TF classification the tool 
		converts the given protein sequence into a feature vector representing the 
		domains detected in the query sequence using the tool InterProScan. 
		Next, a supervised machine learning-based classifier is used to discriminate 
		TFs from Non-TFs based on the computed domain features. Another classifier is 
		then used to predict the structural superclass for protein sequences classified as TFs. 
		In the last step, the domains detected by IPRscan are filtered under consideration of 
		relevant GO-terms to identify the DNA-binding domains of a given TF. TFpredict 
		is particularly useful in combination with SABINE, a related tool which predicts the 
		DNA-motif bound by a transcription factor, given its amino acid sequence, superclass, 
		DNA-binding domains and organism. 

  INPUT:  	FASTA file: 
  		contains the protein identifiers and sequences in FASTA format  (see Format Specification)
  		
  OUTPUT:	SABINE input file:
  	  	contains all information required for post-processing the results with SABINE (see Format Specification)
		(see http://www.cogsys.cs.uni-tuebingen.de/software/SABINE).
		The output filename can be specified by the user (see OPTIONS: -sabineOutfile).
		The argument "-species" also has to be specified if an output file for SABINE
		shall be created.
  
  USAGE:	java -jar TFpredict.jar <input_filename> [OPTIONS]

  OPTIONS : 	-sabineOutfile  <output_filename>	Output file for post-processing of the results with SABINE.

            	-species        <organism_name>		Organism name (e.g., Homo sapiens). See list of supported organisms:
													http://www.cogsys.cs.uni-tuebingen.de/software/SABINE/doc/organism_list.txt

				-tfClassifier <classifier_name>     Classifier used for TF/non-TF classification
													possible values: SVM_linear, NaiveBayes, KNN, Kstar
				
				-superClassifier <classifier_name>  Classifier used for superclass prediction
													possible values: SVM_linear, NaiveBayes, KNN, Kstar	

            	-iprscanPath    <path_to_iprscan>   Path to "iprscan" executable from local InterProScan installation.
													Only needed if you have a local installation of InterProScan which 
													shall be used by TFpredict.

  A full documentation including a tutorial is available at the supplementary website of TFpredict:
  http://www.cogsys.cs.uni-tuebingen.de/software/TFpredict/ 

____________________________________________________________________________________________________________________________

  --------------------
  Format specification
  --------------------


  FASTA file:
  __________________________________________________________________________________________
  >Sequence_1
  MEEPQSDPSVEPPLSQETFSDLWKLLPENNVLSPLPSQAMDDLMLSPDDIEQWFTEDPGP
  DEAPRMPEAAPPVAPAPAAPTPAAPAPAPSWPLSSSVPSQKTYQGSYGFRLGFLHSGTAK
  SVTCTYSPALNKMFCQLAKTCPVQLWVDSTPPPGTRVRAMAIYKQSQHMTEVVRRCPHHE
  RCSDSDGLAPPQHLIRVEGNLRVEYLDDRNTFRHSVVVPYEPPEVGSDCTTIHYNYMCNS
  SCMGGMNRRPILTIITLEDSSGNLLGRNSFEVRVCACPGRDRRTEEENLRKKGEPHHELP
  PGSTKRALPNNTSSSPQPKKKPLDGEYFTLQIRGRERFEMFRELNEALELKDAQAGKEPG
  GSRAHSSHLKSKKGQSTSRHKKLMFKTEGPDSD

  >Sequence_2
  ...

  >Sequence_3
  ...

  SABINE input file:
  __________________________________________________________________________________________
  NA  Identifier
  XX
  SP  Organism
  XX
  CL  Classification (decimal classification no. as in TRANSFAC)
  XX
  S1  Amino acid sequence
  XX
  FT  DNA-binding domain (InterPro ID   start position   end position)
  XX
  //
  XX

  ____________________________________________________________________________________________________________________________

  --------
  Citation
  --------

  If you use TFpredict in any published work, please cite the following article:

  @UNPUBLISHED{eichner2012,
	author = {Johannes Eichner and Florian Topf and Andreas Dr\"ager and Clemens Wrzodek 
		  	  and Dierk Wanke and Andreas Zell},
	title = {{TFpredict and SABINE: Support Vector Machines-based prediction of structural and functional characteristics 
	          of transcription factors}},
	note = {Submitted to Bioinformatics},
	year = {2012}
  }

  ____________________________________________________________________________________________________________________________

  ---------------------
  Website and questions
  ---------------------

  To obtain more detailed information about TFpredict, see the website: http://www.cogsys.cs.uni-tuebingen.de/software/TFpredict/ 
  If you have any further questions, please contact me by e-mail:    	johannes.eichner@uni-tuebingen.de


