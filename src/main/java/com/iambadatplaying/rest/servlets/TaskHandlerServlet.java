package com.iambadatplaying.rest.servlets;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.iambadatplaying.Starter;
import com.iambadatplaying.tasks.*;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Servlet that handles tasks
 */
@Path("/tasks")
public class TaskHandlerServlet {

    private static final String CLASS_EXTENSION = ".class";

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadTask(
            @FormDataParam("file") InputStream inputStream,
            @FormDataParam("file") FormDataContentDisposition fileDetail
    ) {
        Starter starter = Starter.getInstance();
        ReworkedTaskManager taskManager = starter.getTaskManager();
        if (taskManager == null) {
            return Response
                    .status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(ServletUtils.createErrorJson("TaskManager not referenced", "TaskManager not referenced, please wait and try again"))
                    .build();
        }

        ReworkedTaskLoader taskLoader = taskManager.getTaskLoader();
        if (taskLoader == null) {
            return Response
                    .status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(ServletUtils.createErrorJson("TaskLoader not referenced", "TaskLoader not referenced, please wait and try again"))
                    .build();
        }

        if (inputStream == null || fileDetail == null) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(ServletUtils.createErrorJson("Invalid file", "File cannot be null"))
                    .build();
        }

        String fileName = fileDetail.getFileName();
        if (fileName == null) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(ServletUtils.createErrorJson("Invalid file", "File name cannot be null"))
                    .build();
        }

        if (!fileName.endsWith(CLASS_EXTENSION)) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(ServletUtils.createErrorJson("Invalid file", "File must be a .class file"))
                    .build();
        }

        String taskName = fileName.substring(0, fileName.length() - CLASS_EXTENSION.length());
        try {
            Files.copy(inputStream, taskManager.getTaskDir().resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            e.printStackTrace();
            return Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ServletUtils.createErrorJson("Failed to save file", "Failed to save file"))
                    .build();
        }

        taskLoader.loadTask(taskManager.getTaskDir().resolve(fileName));

        JsonObject response = new JsonObject();
        response.addProperty("taskName", taskName);

        return Response
                .status(Response.Status.CREATED)
                .entity(response)
                .build();
    }

    @GET
    @Produces("application/json")
    public Response getAvailableTasks() {
        Starter starter = Starter.getInstance();
        ReworkedTaskManager taskManager = starter.getTaskManager();
        if (taskManager == null) {
            return Response
                    .status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(ServletUtils.createErrorJson("TaskManager not referenced", "TaskManager not referenced, please wait and try again"))
                    .build();
        }

        if (!taskManager.isRunning()) {
            return Response
                    .status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(ServletUtils.createErrorJson("TaskManager not running", "TaskManager not running, please wait for League to start and try again"))
                    .build();
        }

        return Response.status(Response.Status.OK).entity(taskManager.getTaskList()).build();
    }

    /**
     * Get the status and current configuration of a task by name
     */
    @GET
    @Produces("application/json")
    @Path("/{taskName}")
    public Response getTask(@PathParam("taskName") String taskName) {
        Starter starter = Starter.getInstance();
        ReworkedTaskManager taskManager = starter.getTaskManager();
        if (taskManager == null) {
            return Response
                    .status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(ServletUtils.createErrorJson("TaskManager not referenced", "TaskManager not referenced, please wait and try again"))
                    .build();
        }

        if (!taskManager.isRunning()) {
            return Response
                    .status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(ServletUtils.createErrorJson("TaskManager not running", "TaskManager not running, please wait for League to start and try again"))
                    .build();
        }


        Class<? extends Task> taskClass = taskManager.getTaskClass(taskName);
        if (taskClass == null) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(ServletUtils.createErrorJson("Task not found", "Task not found"))
                    .build();
        }


        JsonObject taskJson = new JsonObject();

        Task task = taskManager.getRunningTask(taskName);
        if (task != null) {
            taskJson.add(Task.KEY_TASK_CURRENT_VALUES, task.getCurrentValues());
            taskJson.addProperty(Task.KEY_TASK_RUNNING, true);
        } else {
            taskJson.add(Task.KEY_TASK_CURRENT_VALUES, new JsonObject());
            taskJson.addProperty(Task.KEY_TASK_RUNNING, false);
        }

        taskJson.addProperty(Task.KEY_TASK_NAME, taskClass.getSimpleName());
        taskJson.addProperty(Task.KEY_TASK_DESCRIPTION, Task.getTaskDescription(taskClass));
        taskJson.add(Task.KEY_TASK_ARGUMENTS, Task.getTaskArguments(taskClass));
        return Response.status(Response.Status.OK).entity(taskJson.toString()).build();
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/{taskName}")
    public Response startTask(@PathParam("taskName") String taskName, JsonElement jsonElement) {
        Starter starter = Starter.getInstance();
        ReworkedTaskManager taskManager = starter.getTaskManager();
        if (taskManager == null) {
            return Response.status(Response
                            .Status.SERVICE_UNAVAILABLE)
                    .entity(ServletUtils.createErrorJson("TaskManager not referenced", "TaskManager not referenced, please wait and try again"))
                    .build();
        }

        if (!taskManager.isRunning()) {
            return Response
                    .status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(ServletUtils.createErrorJson("TaskManager not running", "TaskManager not running, please wait for League to start and try again"))
                    .build();
        }

        if (!jsonElement.isJsonObject()) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(ServletUtils.createErrorJson("Invalid JSON", "All Data should be a JSON object"))
                    .build();
        }

        JsonObject json = jsonElement.getAsJsonObject();

        boolean success = taskManager.startOrChangeTask(taskName, json);
        if (!success) {
            return Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ServletUtils.createErrorJson("Failed to set task arguments", "Task failed to set arguments"))
                    .build();
        }

        return Response.status(Response.Status.OK).build();
    }

    @PUT
    @Path("/{taskName}")
    public Response modifyTask(@PathParam("taskName") String taskName, JsonElement body) {
        return startTask(taskName, body);
    }

    @DELETE
    @Path("/{taskName}")
    public Response stopTask(@PathParam("taskName") String taskName) {
        Starter starter = Starter.getInstance();
        ReworkedTaskManager taskManager = starter.getTaskManager();
        if (taskManager == null) {
            return Response.status(Response
                            .Status.SERVICE_UNAVAILABLE)
                    .entity(ServletUtils.createErrorJson("TaskManager not referenced", "TaskManager not referenced, please wait and try again"))
                    .build();
        }

        if (!taskManager.isRunning()) {
            return Response
                    .status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(ServletUtils.createErrorJson("TaskManager not running", "TaskManager not running, please wait for League to start and try again"))
                    .build();
        }

        if (taskName == null) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(ServletUtils.createErrorJson("Invalid task name", "Task name cannot be null"))
                    .build();
        }

        boolean success = taskManager.stopTask(taskName);
        if (!success) {
            return Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ServletUtils.createErrorJson("Failed to stop task", "Task failed to stop"))
                    .build();
        }

        return Response.status(Response.Status.NO_CONTENT).build();
    }
}
