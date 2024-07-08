package com.example.shiftmanagement.controller;

import com.example.shiftmanagement.model.EmployeeShift;
import com.example.shiftmanagement.service.EmployeeShiftService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/shift")
public class EmployeeShiftController {

    @Autowired
    private EmployeeShiftService employeeShiftService;

    @PostMapping("/upload")
    public void uploadShiftData(@RequestParam("month") String month, @RequestParam("year") int year, @RequestParam("file") MultipartFile file) {
        employeeShiftService.saveShiftData(month, year, file);
    }

    @GetMapping("/all")
    public List<EmployeeShift> getAllEmployeeShifts(@RequestParam("month") String month, @RequestParam("year") int year) {
        return employeeShiftService.getAllEmployeeShifts(month, year);
    }
    
    
    @GetMapping("/search/{employeeName}")
    public ResponseEntity<List<String>> findByEmployeeName(@PathVariable String employeeName) {
        List<String> employeeShifts = employeeShiftService.findByEmployeeName(employeeName);
        if (employeeShifts != null && !employeeShifts.isEmpty()) {
            return ResponseEntity.ok(employeeShifts);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/details/{employeeName}")
    public ResponseEntity<EmployeeShift> getEmployeeDetails(@RequestParam("month") String month, @RequestParam("year") int year, @PathVariable String employeeName) {
        EmployeeShift employeeShift = employeeShiftService.getEmployeeDetails(month, year, employeeName);
        if (employeeShift != null) {
            return ResponseEntity.ok(employeeShift);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
