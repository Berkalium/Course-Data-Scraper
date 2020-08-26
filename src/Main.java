import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class Main {

    static int academicYear = 2020;
    static String studySession = "fw";
    static String[] AP_SUBJECTS = {
            /*"ADMS",
            "ANTH",
            "ARB%20",
            "ASL%20",
            "CCY%20",
            "CH%20%20",
            "CLST",
            "CLTR",
            "COGS",
            "COMN",
            "CRIM",
            "DEMS",
            "DLLL",
            "ECON",
            "EN%20%20",
            "ESL%20",
            "FR%20%20",
            "GCIN",
            "GEOG",
            "GER%20",
            "GK%20%20",
            "GKM%20",
            "GWST",
            "HEB%20",
            "HIST",
            "HND%20",
            "HREQ",
            "HRM%20",
            "HUMA",
            "INDG",
            "IT%20%20",
            "ITEC",
            "JC%20%20",
            "JP%20%20",
            "KOR%20",
            "LA%20%20",
            "LASO",
            "LING",
            "LLS%20",
            "MIST",
            "MODR",
            "PERS",
            "PHIL",
            "POLS",
            "POR%20",
            "PPAS",
            "PRWR",
            "SOCI",
            "SOSC",
            "SOWK",
            "SP%20%20",
            "SWAH",
            "SXST",
            "TESL",
            "TYP%20",
            "WKLS",
            "WRIT"*/
            "ECON"
    };

    public static void main(String[] args) throws IOException, SQLException {
        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/maindb","root","");
        conn.createStatement().executeUpdate("TRUNCATE course_information");
        conn.createStatement().executeUpdate("TRUNCATE course_sections");
        conn.createStatement().executeUpdate("TRUNCATE course_sections_sessions");
        Map<String, String[]> coursesIndex = new HashMap<>();
        coursesIndex.put("AP", AP_SUBJECTS);
        int coursesCount = 0;
        int coursesProcessed = 0;
        for (Map.Entry<String, String[]> e : coursesIndex.entrySet()) {
            String faculty = e.getKey();
            String[] subjects  = e.getValue();
            for (String subject : subjects) {
                Document courseIndex = Jsoup.connect("https://w2prod.sis.yorku.ca/Apps/WebObjects/cdm.woa/wa/crsq1?faculty=" + faculty + "&subject=" + subject + "&academicyear=" + academicYear + "&studysession=" + studySession).maxBodySize(0).get();
                Elements courseIndexRows = courseIndex.select("tr[bgcolor=#cccccc]").get(0).parent().select("> tr:gt(0)");
                int courseIndexRowsCount = courseIndexRows.size();
                coursesCount += courseIndexRowsCount;
                int i = 0;
                for (Element courseIndexRow : courseIndexRows) {
                    String courseIndexRowURL = "https://w2prod.sis.yorku.ca" + courseIndexRow.select("> td").get(2).select("a").attr("href");
                    Document CID = Jsoup.connect(courseIndexRowURL).maxBodySize(0).get();
                    String CIDHeading = CID.select("p[class=heading]").text();
                    String courseFaculty = (CIDHeading.split(" ")[0]).split("/")[0];
                    String courseSubject = (CIDHeading.split(" ")[0]).split("/")[1];
                    String courseNumber = CIDHeading.split(" ")[1];
                    String courseCredit = CIDHeading.split(" ")[2];
                    String courseName = courseIndexRow.select("> td").get(1).text();
                    String courseCode = courseFaculty + "/" + courseSubject + " " + courseNumber + " " + courseCredit;
                    String courseCrosslisted = (CID.select("p:contains(Crosslisted to)").size() > 0) ? CID.select("p:contains(Crosslisted to)").get(0).text().substring(17) : "";
                    String courseDescription = (CID.select("b:contains(Course Description)").size() > 0) ? CID.select("b:contains(Course Description)").get(0).parent().nextElementSibling().text() : "";
                    String courseInstructionLanguage = (CID.select("b:contains(Language of Instruction)").size() > 0) ? CID.select("b:contains(Language of Instruction)").get(0).parent().nextElementSibling().text() : "";
                    System.out.println(courseCode + " - " + courseName);
                    if (courseCrosslisted.length() > 0) {
                        courseCrosslisted = courseCrosslisted.substring(0, courseCrosslisted.length() - 1);
                        System.out.println("\tCrosslisted to: " + courseCrosslisted.substring(0, courseCrosslisted.length() - 1));
                    }

                    PreparedStatement courseInformationInsertPS = conn.prepareStatement("INSERT IGNORE INTO course_information (course_faculty, course_subject, course_name, course_description, course_number, course_credit, course_code, course_crosslisted, course_instruction_language) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS);
                    courseInformationInsertPS.setString(1, courseFaculty);
                    courseInformationInsertPS.setString(2, courseSubject);
                    courseInformationInsertPS.setString(3, courseName);
                    courseInformationInsertPS.setString(4, courseDescription);
                    courseInformationInsertPS.setString(5, courseNumber);
                    courseInformationInsertPS.setString(6, courseCredit);
                    courseInformationInsertPS.setString(7, courseCode);
                    courseInformationInsertPS.setString(8, courseCrosslisted);
                    courseInformationInsertPS.setString(9, courseInstructionLanguage);
                    courseInformationInsertPS.execute();
                    System.out.println();
                    Elements courseSections = CID.select("td[bgcolor=#CC0000][class=bodytext][width=50%]");
                    for (Element courseSection : courseSections) {
                        PreparedStatement courseSectionsInsertPS = conn.prepareStatement("INSERT IGNORE INTO course_sections (RelatedCourseID, SectionTerm, SectionLetter, SectionDirector) VALUES (?, ?, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS);
                        System.out.println("\t" + courseSection.text());
                        String courseSectionTerm = courseSection.text().split(" ")[1];
                        String courseSectionLetter = courseSection.text().split(" ")[3];
                        /*String courseSectionAvailabilityURL = "https://w2prod.sis.yorku.ca" + courseSection.parent().nextElementSibling().select("td > a").attr("href");
                        System.out.println("\t\tAvailability URL: " + courseSectionAvailabilityURL);*/
                        String courseSectionDirector = courseSection.parent().nextElementSibling().select("td").get(0).textNodes().get(0).text().substring(18);
                        System.out.println("\t\tSection Director: " + courseSectionDirector);
                        ResultSet rs = courseInformationInsertPS.getGeneratedKeys();
                        if (rs.next()) {
                            courseSectionsInsertPS.setInt(1, rs.getInt(1));
                        }
                        courseSectionsInsertPS.setString(2, courseSectionTerm);
                        courseSectionsInsertPS.setString(3, courseSectionLetter);
                        courseSectionsInsertPS.setString(4, courseSectionDirector);
                        courseSectionsInsertPS.execute();

                        Elements courseSectionSessionsRows = courseSection.parent().nextElementSiblings().get(1).select("tbody").get(0).select("> tr:gt(0)");
                        for (Element courseSectionSessionRow : courseSectionSessionsRows) {
                            Elements courseSectionSessionRowCells = courseSectionSessionRow.select("> td");
                            String courseSectionSessionRowCellType = courseSectionSessionRowCells.get(0).text();
                            System.out.print("\t\t\t\t" + courseSectionSessionRowCellType + " | ");
                            if (courseSectionSessionRowCells.get(1).select("tbody").size() > 0) {
                                Elements courseSectionScheduleRowDayStartTimeDurationLocations = courseSectionSessionRowCells.get(1).select("tbody").get(0).select("> tr");
                                for (Element courseSectionScheduleRowDayStartTimeDurationLocation : courseSectionScheduleRowDayStartTimeDurationLocations) {
                                    System.out.print("(" + courseSectionScheduleRowDayStartTimeDurationLocation.text() + ")");
                                }
                            } else {
                                System.out.print("");
                            }
                            String courseSectionSessionRowCatalogueNumber = courseSectionSessionRowCells.get(2).text();
                            System.out.print(" | " + courseSectionSessionRowCatalogueNumber);
                            String courseSectionSessionRowInstructor = courseSectionSessionRowCells.get(3).text();
                            System.out.print(" | " + courseSectionSessionRowInstructor);
                            String courseSectionSessionRowNotes = courseSectionSessionRowCells.get(4).text();
                            System.out.print(" | " + courseSectionSessionRowNotes + " |");
                            System.out.println();
                            PreparedStatement courseSectionsSessionsInsertPS = conn.prepareStatement("INSERT IGNORE INTO course_sections_sessions (RelatedSectionID, SessionType, SessionCatNumber, SessionInstructor, SessionNotes) VALUES (?, ?, ?, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS);
                            ResultSet rs2 = courseSectionsInsertPS.getGeneratedKeys();
                            if (rs2.next()) {
                                courseSectionsSessionsInsertPS.setInt(1, rs2.getInt(1));
                            }
                            courseSectionsSessionsInsertPS.setString(2, courseSectionSessionRowCellType);
                            courseSectionsSessionsInsertPS.setString(3, courseSectionSessionRowCatalogueNumber);
                            courseSectionsSessionsInsertPS.setString(4, courseSectionSessionRowInstructor);
                            courseSectionsSessionsInsertPS.setString(5, courseSectionSessionRowNotes);
                            courseSectionsSessionsInsertPS.execute();

                        }
                    }

                    coursesProcessed++;
                    System.out.println();
                    System.out.printf("Progress: %d / %d", coursesProcessed, coursesCount);
                    System.out.println();
                }
            }
        }
    }

}
