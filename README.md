TFpredict
=========
<img align="right" src="https://github.com/draeger-lab/TFpredict/blob/master/doc/tfpredict_logo.png" title="TFpredict"/> 

**Identification and structural characterization of transcription factors based on supervised machine learning**

[![License (GPL version 3)](https://img.shields.io/badge/license-GPLv3.0-blue.svg?style=plastic)](http://opensource.org/licenses/GPL-3.0)
[![Stable version](https://img.shields.io/badge/Stable_version-1.3-brightgreen.svg?style=plastic)](https://github.com/draeger-lab/TFpredict/releases/)
[![DOI](http://img.shields.io/badge/DOI-10.1371%20%2F%20journal.pone.0082238-blue.svg?style=plastic)](http://dx.doi.org/10.1371/journal.pone.0082238)

*Authors:* [Johannes Eichner](https://github.com/jeichner/), Florian Topf, [Andreas Dräger](https://github.com/draeger/), [James T. Yurkovich](https://github.com/jtyurkovich/), [Michael Römer](https://github.com/mroemer/)
___________________________________________________________________________________________________________


Article citations are **critical** for us to be able to continue support for TFpredict.  If you use TFpredict and you publish papers about work that uses TFpredict, we ask that you **please cite the TFpredict paper**.

<dl>
  <dd>Johannes Eichner, Florian Topf, Andreas Dräger, Clemens Wrzodek, Dierk Wanke, and Andreas Zell. <a href="http://dx.doi.org/10.1371%2Fjournal.pone.0082238">TFpredict and SABINE: Sequence-Based Prediction of Structural and Functional Characteristics of Transcription Factors</a>. PLoS ONE, 8(12):e82238, December 2013.
  [ <a href="http://dx.doi.org/10.1371/journal.pone.0082238">DOI:10.1371/journal.pone.0082238</a> | <a href="http://www.plosone.org/article/fetchObject.action?uri=info%3Adoi%2F10.1371%2Fjournal.pone.0082238&representation=PDF">PDF</a> ]  
  </dd>
</dl>

  ___________________________________________________________________________________________________________

**TFpredict** is a tool which implements a novel three-step classification method which expects a protein sequence as input and (1) distinguishes transcription factors (TF) from other proteins (Non-TF), (2) predicts the structural superclass of TFs (see TransFac classification), and (3) identifies the DNA-binding domains of TFs. The latter two classification steps are only to be performed if the given protein sequence was identified as a TF. The tool incorporates the results from a [BLAST+](http://blast.ncbi.nlm.nih.gov/Blast.cgi?CMD=Web&PAGE_TYPE=BlastDocs&DOC_TYPE=Download) search into a novel feature representation which allows TF/non-TF classification by state-of-the-art machine learning methods. Specific supervised classifiers were constructed for the task of identifying TFs and their structural superclasses, respectively. Next, known protein domains are detected by the tool [InterProScan](http://www.ebi.ac.uk/Tools/pfa/iprscan/), and then the DNA-binding domains among these are filtered through GO-terms. TFpredict was implemented as an additional preprocessing tool for [SABINE](http://www.cogsys.cs.uni-tuebingen.de/software/SABINE), which predicts the DNA-motif bound by a transcription factor, given its amino acid sequence, superclass, DNA-binding domains and organism.
  ___________________________________________________________________________________________________________
  
  Availability
  ------------
  
TFpredict is available in two different versions for classification of
1. eukaryotic transcription factors ([master branch](https://github.com/draeger-lab/TFpredict/tree/master))
2. prokaryotic σ-factors ([prokaryote branch](https://github.com/draeger-lab/TFpredict/tree/prokaryote))


Table of Contents
-----------------

 - [Installation](#-Installation)
 - [Manual](#-Manual)
 - [Format specification](#-format-and-specification)
 - [Website and questions](#-website-and-questions)
 - [Copyright and license](#-copyright-and-license)
 - [Contact](#-contact)


  Installation
  ------------

  To extract the gzipped tar archive of TFpredict obtained from our downloads section use the command:
  
```
tar -xzf tf_predict.tar.gz
```
  
  TFpredict is completely implemented in Java and provided as a runnable JAR file. All platforms (Windows, Mac, Linux) 
  are supported provided that Java (JDK 1.6 or later) and BLAST (NCBI BLAST 2.2.27+ or later) is installed. 
  You can download the latest version of BLAST from ftp://ftp.ncbi.nlm.nih.gov/blast/executables/blast+/LATEST/. 

  _____________________________________________________________________________________________________________


  Manual
  ------ 

  **INPUT:**  FASTA file: contains the protein identifiers and sequences in FASTA format  (see Format Specification)
  
  **OUTPUT:** SABINE input file: contains all information required for post-processing the results with SABINE (see Format Specification at http://www.cogsys.cs.uni-tuebingen.de/software/SABINE). The output filename can be specified by the user (see OPTIONS: `-sabineOutfile`). The argument `-species` also has to be specified if an output file for SABINE shall be created.
  
  **USAGE:**
  
  ```
  java -jar TFpredict.jar <input_filename> [OPTIONS]
  ```
  
  **OPTIONS:** 	
  * `-sabineOutfile <output_filename>` Output file for post-processing of the results with SABINE.
  * `-species <organism_name>` Organism name (e.g., Homo sapiens). See list of supported organisms: http://www.cogsys.cs.uni-tuebingen.de/software/SABINE/doc/organism_list.txt
  * `-tfClassifier <classifier_name>` Classifier used for TF/non-TF classification possible values: SVM_linear, NaiveBayes, KNN
  * `-superClassifier <classifier_name>` Classifier used for superclass prediction possible values: SVM_linear, NaiveBayes, KNN
  * `-iprscanPath <path_to_iprscan>` Path to "iprscan" executable from local InterProScan installation. Only needed if you have a local installation of InterProScan which shall be used by TFpredict.
  * `-blastPath <path_to_blast>` Path to "bin" directory containing BLAST executables (e.g. /opt/blast/latest). Only needed if environment variable BLAST_PATH is not set.

  ___________________________________________________________________________________________________________

  Format specification
  --------------------

  FASTA file:
  
```
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
```
  
  SABINE input file:
  --------------------

```
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
```

  Copyright and License
  ---------------------

  Copyright (c) 2013 by the individual authors of this software.

  This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
 
  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 
  You should have received a copy of the GNU General Public License along with this program; if not, see http://www.gnu.org/licenses/.

  _____________________________________________________________________________________________________________

  Contact
  ---------------------

  In case of any questions, please contact <a href="mailto:andreas.draeger@uni-tuebingen.de?subject=TFpredict">Andreas Dräger</a>.
