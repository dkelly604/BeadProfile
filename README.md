# BeadProfile
ImageJ plugin to measure a field of beads and apply line profiles across each identified single bead. The output is a text file showing the intensity of the 2 highest peaks in the profile. On starting the plugin asks the user to open 2 tiff images and then identify which is red and green. The plugin will automatically identify the round beads within a binary mask created from the green image, using the centroid of all beads which meet the acceptance criteria a line profile in the North-South, East-West directions and as a forward and backward facing line are placed onto the original red and green images. The values along the length of the line in both colours are measured and the 2 highest values output to a text file for Excel, R or Graphpad. An image is produced with each bead numbered so that the line profiles can be linked with the bead from which they came.

INSTALLATION

1.Ensure that the ImageJ version is at least 1.5 and the installation has Java 1.8.0_60 (64bit) or higher installed. If not download the latest version of ImageJ bundled with Java and install it.

 2.The versions can be checked by opening ImageJ and clicking Help then About ImageJ.

 3.Download the latest copy of Bio-Formats into the ImageJ plugin directory if you are using images not in the tiff format.

 4.Create a directory in the C: drive called Temp (case sensitive)

 5.Using notepad save a blank .txt files called Results.txt into the Temp directory you previously created (also case sensitive).

 6.Place RingMeasure_.jar into the plugins directory of your ImageJ installation, a plugin called Dots Lines should appear in the Plugins drop down menu on ImageJ.

 7.RingMeasure_.java is the editable code for the plugin should improvements or changes be required.
