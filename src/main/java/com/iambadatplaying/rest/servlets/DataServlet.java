package com.iambadatplaying.rest.servlets;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.iambadatplaying.Starter;
import com.iambadatplaying.data.DataManager;
import com.iambadatplaying.data.array.ArrayDataManager;
import com.iambadatplaying.data.map.MapDataManager;
import com.iambadatplaying.data.state.StateDataManager;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.Optional;

@Path("/managers")
public class DataServlet {
    @GET
    @Path("/map")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAvailableMapDataManagers() {
        Starter starter = Starter.getInstance();
        DataManager dataManager = starter.getDataManager();
        if (dataManager == null) {
            return Response
                    .status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(ServletUtils.createErrorJson("DataManager not referenced", "DataManager not referenced, please wait and try again"))
                    .build();
        }


        Collection<MapDataManager<?>> mapDataManagers = dataManager.getMapManagers();

        if (mapDataManagers == null || mapDataManagers.isEmpty()) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(ServletUtils.createErrorJson("No MapDataManagers found", "No MapDataManagers found"))
                    .build();
        }

        JsonArray jsonArray = new JsonArray();
        for (MapDataManager<?> mapDataManager : mapDataManagers) {
            jsonArray.add(mapDataManager.getClass().getName());
        }

        return Response
                .status(Response.Status.OK)
                .entity(jsonArray)
                .build();
    }

    @GET
    @Path("/map/{mapDataManager}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMapData(@PathParam("mapDataManager") String mapDataManagerName) {
        Starter starter = Starter.getInstance();
        DataManager dataManager = starter.getDataManager();
        if (dataManager == null) {
            return Response
                    .status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(ServletUtils.createErrorJson("DataManager not referenced", "DataManager not referenced, please wait and try again"))
                    .build();
        }

        Optional<Class<?>> mapDataManagerClass = classForName(mapDataManagerName);
        if (!mapDataManagerClass.isPresent()) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(ServletUtils.createErrorJson("Invalid MapDataManager", "Invalid MapDataManager"))
                    .build();
        }

        MapDataManager<?> mapDataManagerInstance = dataManager.getMapManager(mapDataManagerClass.get());
        if (mapDataManagerInstance == null) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(ServletUtils.createErrorJson("MapDataManager not found", "MapDataManager not found"))
                    .build();
        }

        return Response
                .status(Response.Status.OK)
                .entity(mapDataManagerInstance.getMapAsJson())
                .build();
    }

    @GET
    @Path("/map/{mapDataManager}/{key}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMapData(@PathParam("mapDataManager") String mapDataManagerName, @PathParam("key") String key) {
        Starter starter = Starter.getInstance();
        DataManager dataManager = starter.getDataManager();
        if (dataManager == null) {
            return Response
                    .status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(ServletUtils.createErrorJson("DataManager not referenced", "DataManager not referenced, please wait and try again"))
                    .build();
        }

        Optional<Class<?>> mapDataManagerClass = classForName(mapDataManagerName);
        if (!mapDataManagerClass.isPresent()) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(ServletUtils.createErrorJson("Invalid MapDataManager", "MapDataManager " + mapDataManagerName + " not found"))
                    .build();
        }

        MapDataManager<?> mapDataManagerInstance = dataManager.getMapManager(mapDataManagerClass.get());
        if (mapDataManagerInstance == null) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(ServletUtils.createErrorJson("MapDataManager not found", "MapDataManager not found"))
                    .build();
        }


        Optional<JsonObject> optResult = mapDataManagerInstance.getExternal(key);

        if (!optResult.isPresent()) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(ServletUtils.createErrorJson("Key not found", "Key " + key + " not found"))
                    .build();
        }

        return Response
                .status(Response.Status.OK)
                .entity(optResult.get())
                .build();
    }

    @GET
    @Path("/state")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAvailableStateDataManagers() {
        Starter starter = Starter.getInstance();
        DataManager dataManager = starter.getDataManager();
        if (dataManager == null) {
            return Response
                    .status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(ServletUtils.createErrorJson("DataManager not referenced", "DataManager not referenced, please wait and try again"))
                    .build();
        }

        Collection<StateDataManager> stateDataManagers = dataManager.getStateManagers();

        if (stateDataManagers == null || stateDataManagers.isEmpty()) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(ServletUtils.createErrorJson("No StateDataManagers found", "No StateDataManagers found"))
                    .build();
        }

        JsonArray jsonArray = new JsonArray();
        for (StateDataManager stateDataManager : stateDataManagers) {
            jsonArray.add(stateDataManager.getClass().getName());
        }

        return Response
                .status(Response.Status.OK)
                .entity(jsonArray)
                .build();
    }

    @GET
    @Path("/state/{stateDataManager}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCurrentState(@PathParam("stateDataManager") String stateDataManagerName) {
        Starter starter = Starter.getInstance();
        DataManager dataManager = starter.getDataManager();
        if (dataManager == null) {
            return Response
                    .status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(ServletUtils.createErrorJson("DataManager not referenced", "DataManager not referenced, please wait and try again"))
                    .build();
        }

        Optional<Class<?>> stateDataManagerClass = classForName(stateDataManagerName);
        if (!stateDataManagerClass.isPresent()) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(ServletUtils.createErrorJson("Invalid StateDataManager", "StateDataManager " + stateDataManagerName + " not found"))
                    .build();
        }

        StateDataManager stateDataManagerInstance = dataManager.getStateManager(stateDataManagerClass.get());
        if (stateDataManagerInstance == null) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(ServletUtils.createErrorJson("StateDataManager not found", "StateDataManager not found"))
                    .build();
        }

        return Response
                .status(Response.Status.OK)
                .entity(stateDataManagerInstance.getCurrentState().orElse(new JsonObject()))
                .build();
    }

    private Optional<Class<?>> classForName(String className) {
        try {
            return Optional.of(Class.forName(className));
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            return Optional.empty();
        }
    }

    @GET
    @Path("/array")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAvailableArrayDataManagers() {
        Starter starter = Starter.getInstance();
        DataManager dataManager = starter.getDataManager();
        if (dataManager == null) {
            return Response
                    .status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(ServletUtils.createErrorJson("DataManager not referenced", "DataManager not referenced, please wait and try again"))
                    .build();
        }

        Collection<ArrayDataManager> arrayDataManagers = dataManager.getArrayManagers();

        if (arrayDataManagers == null || arrayDataManagers.isEmpty()) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(ServletUtils.createErrorJson("No ArrayDataManagers found", "No ArrayDataManagers found"))
                    .build();
        }

        JsonArray jsonArray = new JsonArray();
        for (ArrayDataManager arrayDataManager : arrayDataManagers) {
            jsonArray.add(arrayDataManager.getClass().getName());
        }

        return Response
                .status(Response.Status.OK)
                .entity(jsonArray)
                .build();
    }

    @GET
    @Path("/array/{arrayDataManager}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getArrayData(@PathParam("arrayDataManager") String arrayDataManagerName) {
        Starter starter = Starter.getInstance();
        DataManager dataManager = starter.getDataManager();
        if (dataManager == null) {
            return Response
                    .status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(ServletUtils.createErrorJson("DataManager not referenced", "DataManager not referenced, please wait and try again"))
                    .build();
        }

        Optional<Class<?>> arrayDataManagerClass = classForName(arrayDataManagerName);
        if (!arrayDataManagerClass.isPresent()) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(ServletUtils.createErrorJson("Invalid ArrayDataManager", "Invalid ArrayDataManager"))
                    .build();
        }

        ArrayDataManager arrayDataManagerInstance = dataManager.getArrayManager(arrayDataManagerClass.get());
        if (arrayDataManagerInstance == null) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(ServletUtils.createErrorJson("ArrayDataManager not found", "ArrayDataManager not found"))
                    .build();
        }

        return Response
                .status(Response.Status.OK)
                .entity(arrayDataManagerInstance.getCurrentState().orElse(new JsonArray()))
                .build();
    }
}
