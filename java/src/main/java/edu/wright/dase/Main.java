//package edu.wright.dase;
///*
//Written by sarker.
//Written at 5/7/18.
//*/
//
//import edu.wright.dase.util.Utility;
//import edu.wright.dase.util.Writer;
//import org.semanticweb.owlapi.model.OWLOntology;
//
//import java.nio.file.Files;
//import java.nio.file.Paths;
//
//public class Main {
//
//
//    /**
//     * Main method
//     *
//     * @param args
//     */
//    public static void main(String[] args) {
//
//        int i = 0;
//        Writer.writeInDisk(logFile, "Main Program started at: " + Utility.getCurrentTimeAsString(), false);
//        try {
//            Files.walk(Paths.get(alreadyGotResultPath)).filter(f -> f.toFile().isFile()).
//                    filter(f -> f.toFile().getAbsolutePath().endsWith(".txt")).forEach(f->{
//                alreadyGotResult.add(f.toFile().getName().replaceAll("_expl_pos_only.txt", ".conf"));
//                Writer.writeInDisk(logFile, "\nAlready got result for: " + f.toString(), true);
//            });
//        }catch(Exception ex){
//            Writer.writeInDisk(logFile, "\n\n!!!!!!!Fatal error!!!!!!!\n" + Utility.getStackTraceAsString(ex), true);
//        }
//
//        OWLOntology _onto;
//        try {
//
//            // start with root folder
//            Files.walk(Paths.get(explanationForPath)).filter(d -> d.toFile().isDirectory()).forEach(d -> {
//                try {
//                    iterateOverFolders(d);
//                    Writer.writeInDisk(logFile, "\n\nRunning on folder: "+ d.toString() , true);
//                }catch(Exception ex) {
//                    System.out.println("Error occurred");
//                    ex.printStackTrace();
//                    Writer.writeInDisk(logFile, "\n\n!!!!!!!Fatal error!!!!!!!\n" + Utility.getStackTraceAsString(ex), true);
//                    System.exit(0);
//                }
//            });
//
////			_onto = Utility.loadOntology(new File(backgroundOntology));
////			reasonerFactory = new PelletReasonerFactory();
////			owlReasoner = reasonerFactory.createNonBufferingReasoner(_onto);
////
////			String confFilePath = "/home/sarker/MegaCloud/ProjectHCBD/datas/ning_manual/DL_tensorflow_save_v3_txts_as_dirs_owl_without_score_without_wordnet/Bathroom/bathroom_ADE_train_00000006.conf";
////			readExamplesFromConf(confFilePath);
////			Set<OWLNamedIndividual> allExamples = new HashSet<OWLNamedIndividual>();
////			allExamples.addAll(posExamples);
////			allExamples.addAll(negExamples);
////
////			OWLOntology mod_onto = Utility.removeNonRelatedIndividuals(_onto, owlReasoner, allExamples, null);
////			owlReasoner = reasonerFactory.createNonBufferingReasoner(mod_onto);
////
////			OWLOntology _mod_onto = Utility.removeNonRelatedConcepts(mod_onto,owlReasoner);
////
////			String saveTo = "/home/sarker/MegaCloud/ProjectHCBD/datas/sumo_aligned/without_scores/sumo_with_imgContains_without_score_without_wordnet_minimal_rnr__.owl";
////
////			Utility.saveOntology(_mod_onto, saveTo);
//        }
////		catch (OWLOntologyCreationException e1) {
////			// TODO Auto-generated catch block
////			e1.printStackTrace();
////		}catch (OWLOntologyStorageException e1) {
////			// TODO Auto-generated catch block
////			e1.printStackTrace();
////		}
//        catch (Exception e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//
//
//        if (i == 0)
//            return;
//
//        try {
//
//            long startTime = System.currentTimeMillis();
//            String folderName = Paths.get(explanationForPath).getFileName().toString();
//            String writeTo = Paths.get(explanationForPath).toAbsolutePath().toString() + "/" + folderName + "_expl.txt";
//
//            // runDlConfWritings = runDlConfWritings + "bathroom/" +
//            // "bathroom_run_dl_in_b_folder.txt";
//            logger.info("Program started...........");
//            System.out.println("Program started...........");
//            Writer.writeInDisk(writeTo, "\n Program started...........\n", false);
//
//            init();
//
//            tryToCreateExplanations(Paths.get(explanationForPath), writeTo);
//
//            long endTime = System.currentTimeMillis();
//            Long executionTimeInMinute = (endTime - startTime) / (60 * 1000);
//
//            logger.info("Program finsihed after: " + executionTimeInMinute + " minutes");
//            System.out.println("Program finsihed after: " + executionTimeInMinute + " minutes");
//            Writer.writeInDisk(writeTo, "\n\n\n Program finsihed", true);
//            Writer.writeInDisk(writeTo, "\nProgram run for: " + (endTime - startTime) / 1000 + " seconds", true);
//            Writer.writeInDisk(writeTo, "\nProgram run for: " + executionTimeInMinute + " minutes", true);
//
//            // combine necessary ontologies
//            // combineOntology();
//
//            // create resoner to reason
//            // owlReasoner = reasonerFactory.createNonBufferingReasoner(combinedOntology);
//
//            // totalClasses = combinedOntology.getClassesInSignature().size();
//
//            // for (int i = 0; i < maxNegativeInstances; i++) {
//            // randomClassIndex.add(ThreadLocalRandom.current().nextInt(0, totalClasses));
//            // }
//            //
//            // Files.walk(Paths.get(rootPath)).filter(d -> !d.toFile().isFile()).forEach(d
//            // -> {
//            // try {
//            //
//            // iterateOverFolders(d);
//            //
//            // } catch (ComponentInitException e) {
//            // // TODO Auto-generated catch block
//            // e.printStackTrace();
//            // } catch (OWLOntologyCreationException e) {
//            // // TODO Auto-generated catch block
//            // e.printStackTrace();
//            // } catch (OWLOntologyStorageException e) {
//            // // TODO Auto-generated catch block
//            // e.printStackTrace();
//            // }
//            // });
//
//        } catch (Exception e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//
//    }
//
//}
