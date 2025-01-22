Getting Started with TFpredict
=================================

This section will guide you through the installation, setup, and first steps to using TFpredict.

How to get started
-------------------

The stand-alone version of TFpredict is equipped with a command-line interface which can be used for the batch processing of multiple protein sequences given in FASTA format. For convenience, TFpredict uses the webservice version of InterproScan. Thus, installing the perl stand-alone version of InterProScan (approx. 40GB) is not required.


Installing TFpredict
---------------------

Download the JAR file TFpredict_1.4 from https://github.com/draeger-lab/TFpredict/releases and also the example file test_seq.fasta.

You can also clone this repository and build a new snapshot release using the ant script shipped with this project by executing the following command in the ``dist`` folder:

.. code-block:: bash

   ant -f tfpredict_build.xml

TFpredict is completely implemented in Java and provided as a runnable JAR file. All platforms (Windows, Mac, Linux) are supported provided that Java (22.0.1 or later) and BLAST (NCBI BLAST 2.15.0+ or later) is installed. You can download the latest version of BLAST from https://ftp.ncbi.nlm.nih.gov/blast/executables/blast+/LATEST/.

To install TFpredict, make sure you have the following dependencies:

- `Java™`_ (JDK 22.0.1 or later)
- BLAST (NCBI BLAST 2.15.0+ or later)

The analysis framework of TFpredict is entirely written in Java. Thus, it requires that Java Virtual Machine (JDK version 22.0.1 or newer) is installed on your system.


Installation and configuration of BLAST
----------------------------------------

Follow the instructions at the BLAST home-page for your operating system. For Unix, go to http://www.ncbi.nlm.nih.gov/books/NBK52640/ For Windows, see the instructions http://www.ncbi.nlm.nih.gov/books/NBK52637/.

Having BLAST successfully installed, it is necessary to pass the path to the executable to TFpredict. To this end, define the environment variable

.. code-block:: bash

   BLAST_DIR

on your system to point to the installation directory of your copy of BLAST. This might be, for instance,

+-------------------------------------+------------------+
| Path                                | Operating System |
+-------------------------------------+------------------+
| ``/usr/local/ncbi/blast/``          |       macOS      |
+-------------------------------------+------------------+
| ``/opt/blast/latest/``              |       Linux      |
+-------------------------------------+------------------+
| ``C:\program~\NCBI\blast-x.x.xxx\`` |     MS Windows   |
+-------------------------------------+------------------+

Note that ``x.x.xxx`` stands for some arbitrary version number of BLAST and must be replaced as necessary.

So, the variable ``BLAST_DIR`` could be set to one of the above example folders. In the bash under MacOS you would, e.g., type

.. code-block:: text
   
   export BLAST_DIR=/usr/local/ncbi/blast/

or on Windows in Command Prompt you would use

.. code-block:: text
   
   set BLAST_DIR=C:\program~\NCBI\blast-x.x.xxx\

before executing TFpredict.


Next Steps
----------

Now that you have TFpredict set up, explore the detailed :ref:`how-to-use`!

If you encounter any issues or need further assistance, please refer to other sections of this documentation or feel free to create an issue on our `GitHub repository`_. 


.. _`Java™`: https://www.java.com/en/
.. _`GitHub repository`: https://github.com/draeger-lab/TFpredict/issues