package com.example.shiftmanagement.service;

import com.example.shiftmanagement.exception.EmployeeNotFoundException;
import com.example.shiftmanagement.model.EmployeeShift;
import com.example.shiftmanagement.repository.EmployeeShiftRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class EmployeeShiftService {

    @Autowired
    private EmployeeShiftRepository employeeShiftRepository;

    private static final List<String> VALID_SHIFT_CODES = Arrays.asList("M", "A", "N");

    private static final List<String> INVALID_EMPLOYEE_NAMES = Arrays.asList(
            "G= General", "A=Afternoon", "N=Night", "M=Morning", "OC=On-Call",
            "P=Planned Leave", "H=Holiday", "HL= Half Day Leave", "U=Unplanned", "Legends",
            "C=Comp off", " ", "Shift Timings", "Morning -5 AM to 2:30PM IST",
            "Afternoon- 11:30 AM to 9 PM IST", "Night - 8 PM to 5:30 AM IST"
    );

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("d-MMM");

    @Transactional
    public void saveShiftData(String month, int year, MultipartFile file) {
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            List<EmployeeShift> employeeShifts = new ArrayList<>();

            employeeShiftRepository.deleteSundayShiftsByMonthAndYear(month, year);
            employeeShiftRepository.deleteByMonthAndYear(month, year);

            if (!rows.hasNext()) return; // Ensure there is at least one row
            Row datesRow = rows.next(); // Read the first row (dates)

            if (!rows.hasNext()) return; // Ensure there is a second row
            Row dayNamesRow = rows.next(); // Read the second row (day names)

            while (rows.hasNext()) {
                Row row = rows.next();
                if (row == null || row.getCell(0) == null || row.getCell(0).getCellType() != CellType.STRING) continue;
                
                // Check for empty or invalid employee name
                String employeeName = row.getCell(0).getStringCellValue().trim();
                if (employeeName.isEmpty() || INVALID_EMPLOYEE_NAMES.contains(employeeName)) continue;

                EmployeeShift employeeShift = new EmployeeShift();
                employeeShift.setEmployeeName(employeeName);
                employeeShift.setMonth(month);
                employeeShift.setYear(year);
                int morningShiftCount = 0, afternoonShiftCount = 0, nightShiftCount = 0;
                int sundayCount = 0;
                Map<String, String> sundayShifts = new HashMap<>();

                boolean hasValidShift = false; // Flag to check for valid shifts

                for (int i = 1; i < row.getLastCellNum(); i++) {
                    Cell cell = row.getCell(i);
                    if (cell != null && cell.getCellType() == CellType.STRING) {
                        String shiftType = cell.getStringCellValue().trim();
                        if (VALID_SHIFT_CODES.contains(shiftType)) {
                            hasValidShift = true; // Mark as valid shift

                            // Get the date and day name from the header
                            String dateStr = getCellStringValue(datesRow.getCell(i));
                            String dayName = getCellStringValue(dayNamesRow.getCell(i));

                            // Check if the day is a weekend
                            boolean isWeekend = "Sat".equals(dayName) || "Sun".equals(dayName);
                            if ("Sun".equals(dayName)) {
                                sundayCount++;
                                sundayShifts.put(dateStr, shiftType);
                            }
                            if (!isWeekend) {
                                switch (shiftType) {
                                    case "M":
                                        morningShiftCount++;
                                        break;
                                    case "A":
                                        afternoonShiftCount++;
                                        break;
                                    case "N":
                                        nightShiftCount++;
                                        break;
                                }
                            }
                        }
                    }
                }

                // Skip rows without valid shifts
                if (!hasValidShift) continue;

                double morningRate = getRateForShift("Morning");
                double afternoonRate = getRateForShift("Afternoon");
                double nightRate = getRateForShift("Night");
                double totalMoney = (morningShiftCount * morningRate) + (afternoonShiftCount * afternoonRate) + (nightShiftCount * nightRate);
                employeeShift.setMorningShiftCount(morningShiftCount);
                employeeShift.setAfternoonShiftCount(afternoonShiftCount);
                employeeShift.setNightShiftCount(nightShiftCount);
                employeeShift.setTotalMoney(totalMoney);
                employeeShift.setSundayCount(sundayCount);
                employeeShift.setSundayShifts(sundayShifts);
                employeeShifts.add(employeeShift);
            }
            employeeShiftRepository.saveAll(employeeShifts);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private String getCellStringValue(Cell cell) {
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue();
        } else if (cell.getCellType() == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                return DATE_FORMAT.format(cell.getDateCellValue());
            } else {
                return String.valueOf((int) cell.getNumericCellValue());
            }
        }
        return "";
    }

    public List<EmployeeShift> getAllEmployeeShifts(String month, int year) {
        return employeeShiftRepository.findByMonthAndYear(month, year);
    }


    public EmployeeShift getEmployeeDetails(String month, int year, String employeeName) {
        List<EmployeeShift> employeeShifts = employeeShiftRepository.findByMonthAndYearAndEmployeeName(month, year, employeeName);
        if (employeeShifts.isEmpty()) {
            throw new EmployeeNotFoundException("Employee with name " + employeeName + " not found for " + month + " " + year);
        }
        return employeeShifts.get(0);
    }
    
    public List<String> findByEmployeeName(String employeeName) {
        List<String> employeeShifts = employeeShiftRepository.findByEmployeeName(employeeName);
        if (employeeShifts.isEmpty()) {
            throw new EmployeeNotFoundException("Employee with name " + employeeName + " not found");
        }
        return employeeShifts;
    }


    private double getRateForShift(String shiftType) {
        switch (shiftType) {
            case "Morning":
                return 400.0;
            case "Afternoon":
                return 500.0;
            case "Night":
                return 700.0;
            default:
                return 0.0;
        }
    }
}
