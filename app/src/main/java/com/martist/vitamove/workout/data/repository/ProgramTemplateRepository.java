package com.martist.vitamove.workout.data.repository;

import com.martist.vitamove.programs.ui.model.ProgramTemplate;
import com.martist.vitamove.programs.ui.model.ProgramTemplateDay;
import com.martist.vitamove.programs.ui.model.ProgramTemplateExercise;

import java.util.Date;
import java.util.List;


public interface ProgramTemplateRepository {


    List<ProgramTemplate> getAllPublicTemplates() throws Exception;


    List<ProgramTemplate> getTemplatesByAuthor(String authorId) throws Exception;


    ProgramTemplate getTemplateById(String templateId) throws Exception;


    String createTemplate(ProgramTemplate template) throws Exception;


    void updateTemplate(ProgramTemplate template) throws Exception;


    void deleteTemplate(String templateId) throws Exception;


    List<ProgramTemplateDay> getTemplateDays(String templateId) throws Exception;


    ProgramTemplateDay getTemplateDayById(String dayId) throws Exception;


    String createTemplateDay(ProgramTemplateDay day) throws Exception;


    void updateTemplateDay(ProgramTemplateDay day) throws Exception;


    void deleteTemplateDay(String dayId) throws Exception;


    List<ProgramTemplateExercise> getTemplateDayExercises(String dayId) throws Exception;


    ProgramTemplateExercise getTemplateExerciseById(String exerciseId) throws Exception;


    String createTemplateExercise(ProgramTemplateExercise exercise) throws Exception;


    void updateTemplateExercise(ProgramTemplateExercise exercise) throws Exception;


    void deleteTemplateExercise(String exerciseId) throws Exception;


    String createProgramFromTemplate(String templateId, String userId, String name) throws Exception;


    default String createProgramFromTemplate(String templateId, String userId, String name,
                                             List<Integer> workoutDays, Date startDate) throws Exception {

        return createProgramFromTemplate(templateId, userId, name);
    }
} 