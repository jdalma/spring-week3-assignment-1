package com.codesoom.assignment.controllers;

import com.codesoom.assignment.TaskNotFoundException;
import com.codesoom.assignment.application.TaskService;
import com.codesoom.assignment.dto.ErrorResponse;
import com.codesoom.assignment.models.Task;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
class TaskControllerWebTest {

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private TaskService service;
    @MockBean
    private TaskErrorAdvice advice;

    private final String RESOURCE = "/tasks";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TaskController(service))
                .setControllerAdvice(advice)
                .build();
    }

    void setErrorAdvice(String invalidId){
        Long id = Long.parseLong(invalidId);
        given(service.getTask(id)).willThrow(new TaskNotFoundException(id));
        given(advice.handleNotFound()).willReturn(new ErrorResponse("Task not found"));
    }

    @Nested
    @DisplayName("GET /할 일은")
    class Describe_GET_NonPathVariable{

        @Test
        @DisplayName("응답 200을 반환한다")
        void It_Status200Return() throws Exception {
            mockMvc.perform(get(RESOURCE))
                    .andExpect(status().isOk());
        }

        @Nested
        @DisplayName("할 일이 존재한다면")
        class Context_ExistsTasks{

            private List<Task> tasks;

            @BeforeEach
            void setUp(){
                tasks = new ArrayList<>();
                Task task1 = new Task(1L , "TEST1");
                Task task2 = new Task(2L , "TEST2");
                tasks.add(task1);
                tasks.add(task2);
                given(service.getTasks()).willReturn(tasks);
            }

            @Test
            @DisplayName("할 일들을 JSON형식으로 반환한다")
            void It_AllTaskToJSONReturn() throws Exception {
                mockMvc.perform(get(RESOURCE))
                        .andExpect(status().isOk())
                        .andExpect(content().string(equalTo(mapper.writeValueAsString(tasks))));
            }
        }

        @Nested
        @DisplayName("할 일이 존재하지 않는다면")
        class Context_NotExistsTask{

            private final String EMPTY_ARR = "[]";

            @BeforeEach
            void setUp(){
                given(service.getTasks()).willReturn(new ArrayList<>());
            }

            @Test
            @DisplayName("빈 List를 반환한다")
            void It_EmptyListReturn() throws Exception {
                mockMvc.perform(get(RESOURCE))
                        .andExpect(status().isOk())
                        .andExpect(content().string(equalTo(EMPTY_ARR)));
            }
        }
    }

    @Nested
    @DisplayName("GET /할 일/{id}는")
    class Describe_GET_PathVariable{

        @Nested
        @DisplayName("{id}에 해당하는 할 일이 있다면")
        class Context_ExistsTask{

            private final Task task = new Task(1L , "TEST1");
            private final String validId = "1";

            @BeforeEach
            void setUp() {
                reset(service);
                given(service.getTask(Long.parseLong(validId))).willReturn(task);
            }

            @Test
            @DisplayName("상태 200 , 할 일을 JSON으로 반환한다")
            void It_JsonReturn() throws Exception {
                String content = mapper.writeValueAsString(task);
                mockMvc.perform(get(RESOURCE +"/"+validId))
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(content().string(equalTo(content)));
            }
        }

        @Nested
        @DisplayName("{id}에 해당하는 할 일이 없다면")
        class Context_NotExistsTask{

            private final String invalidId = "1";

            @BeforeEach
            void setUp(){
                reset(service);
                setErrorAdvice(invalidId);
            }

            @Test
            @DisplayName("상태 404 , 할 일을 찾지 못 하는 예외를 던지고 message를 반환한다")
            void It_ErrorResultReturn() throws Exception {
                mockMvc.perform(get(RESOURCE +"/"+ invalidId))
                        .andDo(print())
                        .andExpect(status().isNotFound())
                        .andExpect(content().string(containsString("Task not found")));
            }
        }
    }

    @Nested
    @DisplayName("POST /할 일은")
    class Describe_POST{

        @Nested
        @DisplayName("body에 제목이 존재한다면")
        class Context_ExistsBodyTitle{

            private final Long newId = 3L;
            private final Task newTask = new Task(newId , "UPDATE TITLE 1");

            @BeforeEach
            void setUp(){
                given(service.createTask(newTask)).willReturn(newTask);
            }

            @Test
            @DisplayName("상태 201 , 할 일을 저장하고 저장된 정보를 반환한다")
            void It_SaveTask() throws Exception {
                String content = mapper.writeValueAsString(newTask);

                mockMvc.perform(post(RESOURCE)
                                .content(content)
                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isCreated())
                        .andExpect(content().string(equalTo(content)));
            }
        }

        @Nested
        @DisplayName("body가 비어있다면")
        class Context_NotExistsBody{

            @Test
            @DisplayName("상태 400 , 요청이 잘못 됐다는 예외를 던진다")
            void It_ThrowException() throws Exception {
                mockMvc.perform(post(RESOURCE))
                        .andExpect(status().isBadRequest());
            }
        }
    }

    @Nested
    @DisplayName("PUT,PATCH /할 일/{id}는")
    class Describe_PUT_PATCH{

        @Nested
        @DisplayName("{id}가 존재하지 않는다면")
        class Context_NotExistsId{

            @Test
            @DisplayName("상태 405 , 해당 네트워크 메소드는 허용하지 않는 예외를 던진다")
            void It_ThrowException() throws Exception {
                mockMvc.perform(put(RESOURCE))
                        .andExpect(status().isMethodNotAllowed());

                mockMvc.perform(patch(RESOURCE))
                        .andExpect(status().isMethodNotAllowed());
            }
        }

        @Nested
        @DisplayName("body가 존재하지 않는다면")
        class Context_NotExistsBody{

            private final String taskId = "1";

            @Test
            @DisplayName("상태 400 , 요청이 잘못 됐다는 예외를 던진다")
            void It_ThrowException() throws Exception {
                mockMvc.perform(put(RESOURCE +"/"+taskId))
                        .andExpect(status().isBadRequest());

                mockMvc.perform(patch(RESOURCE +"/"+taskId))
                        .andExpect(status().isBadRequest());
            }
        }

        @Nested
        @DisplayName("{id}에 해당하는 할 일이 없다면")
        class Context_InvalidId{

            private final Task updateTask = new Task();
            private final String invalidId = "1";

            @BeforeEach
            void setUp(){
                reset(service);
                setErrorAdvice(invalidId);
                updateTask.setTitle("Update Title");
            }

            @Test
            @DisplayName("상태 404 , 요청이 잘못 됐다는 예외를 던진다")
            void It_ThrowException() throws Exception {
                String content = mapper.writeValueAsString(updateTask);
                mockMvc.perform(get(RESOURCE +"/"+ invalidId)
                                .content(content))
                        .andDo(print())
                        .andExpect(status().isNotFound())
                        .andExpect(content().string(containsString("Task not found")));
            }
        }

        @Nested
        @DisplayName("{id}와 body가 존재한다면")
        class Context_ExistsIdAndBody{

            private final Task beforeTask = new Task();
            private final Long updateId = 1L;
            @BeforeEach
            void setUp(){
                beforeTask.setTitle("Update Title");
                given(service.updateTask(updateId , beforeTask)).willReturn(beforeTask);
            }

            @Test
            @DisplayName("body에 담긴 제목으로 수정한다")
            void It_UpdateTask() throws Exception {
                String content = mapper.writeValueAsString(beforeTask);
                mockMvc.perform(put(RESOURCE +"/"+updateId)
                                .content(content)
                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andExpect(content().string(equalTo(content)));
            }
        }
    }

    @Nested
    @DisplayName("DELETE /할 일/{id}는")
    class Describe_DELETE{

        @Nested
        @DisplayName("{id}가 존재하지 않는다면")
        class Context_NotExistsId{

            @Test
            @DisplayName("상태 405 , 해당 네트워크 메소드는 허용하지 않는 예외를 던진다")
            void It_ThrowException() throws Exception {
                mockMvc.perform(delete(RESOURCE))
                        .andExpect(status().isMethodNotAllowed());
            }
        }

        @Nested
        @DisplayName("{id}에 해당하는 할 일이 없다면")
        class Context_InvalidId{

            private final String invalidId = "1";

            @BeforeEach
            void setUp(){
                setErrorAdvice(invalidId);
            }

            @Test
            @DisplayName("상태 404 , 할 일을 찾지 못 하는 예외를 던지고 message를 반환한다")
            void It_ThrowException() throws Exception {
                mockMvc.perform(get(RESOURCE +"/"+ invalidId))
                        .andExpect(status().isNotFound())
                        .andExpect(content().string(containsString("Task not found")));
            }
        }

        @Nested
        @DisplayName("{id}에 해당하는 할 일이 존재한다면")
        class Context_ValidId{

            private final Long validId = 1L;

            @Test
            @DisplayName("상태 204를 반환 , 할 일을 삭제한다")
            void It_DeleteTask() throws Exception {
                mockMvc.perform(delete(RESOURCE +"/"+validId))
                        .andExpect(status().isNoContent());
            }
        }
    }
}
