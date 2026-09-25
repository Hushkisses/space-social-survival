package com.hushkisses.spacesurvival.paper;

import com.hushkisses.spacesurvival.core.BootstrapMarker;
import com.hushkisses.spacesurvival.communication.CommunicationPolicy;
import com.hushkisses.spacesurvival.communication.RadioRuntimeState;
import com.hushkisses.spacesurvival.communication.RadioService;
import com.hushkisses.spacesurvival.infection.InfectionService;
import com.hushkisses.spacesurvival.integration.voicechat.SimpleVoiceChatBridge;
import com.hushkisses.spacesurvival.event.DefaultGameEventCatalog;
import com.hushkisses.spacesurvival.event.GameEventContext;
import com.hushkisses.spacesurvival.event.GameEventEngine;
import com.hushkisses.spacesurvival.event.GameEventRegistry;
import com.hushkisses.spacesurvival.event.GameEventRuntimeState;
import com.hushkisses.spacesurvival.facility.DefaultFacilityCatalog;
import com.hushkisses.spacesurvival.facility.FacilityRegistry;
import com.hushkisses.spacesurvival.facility.action.DefaultFacilityActionCatalog;
import com.hushkisses.spacesurvival.facility.action.FacilityActionRegistry;
import com.hushkisses.spacesurvival.facility.engineering.EngineeringFacilityService;
import com.hushkisses.spacesurvival.facility.medical.MedicalFacilityService;
import com.hushkisses.spacesurvival.integration.itemsadder.ItemsAdderBridge;
import com.hushkisses.spacesurvival.lobby.LobbyService;
import com.hushkisses.spacesurvival.objective.DefaultObjectiveCatalog;
import com.hushkisses.spacesurvival.objective.ObjectiveEngine;
import com.hushkisses.spacesurvival.objective.ObjectiveRegistry;
import com.hushkisses.spacesurvival.objective.secret.SecretMissionService;
import com.hushkisses.spacesurvival.paper.config.PluginConfiguration;
import com.hushkisses.spacesurvival.paper.config.PluginConfigurationLoader;
import com.hushkisses.spacesurvival.paper.item.DefaultResourceItemProvider;
import com.hushkisses.spacesurvival.paper.item.ResourceItemProvider;
import com.hushkisses.spacesurvival.paper.lobby.LobbyConnectionListener;
import com.hushkisses.spacesurvival.paper.pvp.ConditionalPvpListener;
import com.hushkisses.spacesurvival.paper.runtime.GameRuntimeService;
import com.hushkisses.spacesurvival.paper.ui.OpeningBriefingUi;
import com.hushkisses.spacesurvival.paper.ui.OpeningUiListener;
import com.hushkisses.spacesurvival.paper.ui.RoleSelectionUi;
import com.hushkisses.spacesurvival.resource.ResourceLedger;
import com.hushkisses.spacesurvival.resource.processing.DefaultProcessingCatalog;
import com.hushkisses.spacesurvival.resource.processing.ProcessingRegistry;
import com.hushkisses.spacesurvival.resource.processing.ProcessingService;
import com.hushkisses.spacesurvival.role.DefaultRoleCatalog;
import com.hushkisses.spacesurvival.role.RoleRegistry;
import com.hushkisses.spacesurvival.role.selection.RoleSelectionService;
import com.hushkisses.spacesurvival.ship.ShipState;
import com.hushkisses.spacesurvival.scenario.InfectionScenarioService;
import com.hushkisses.spacesurvival.scenario.ScenarioEngine;
import com.hushkisses.spacesurvival.social.meeting.MeetingService;
import com.hushkisses.spacesurvival.social.pvp.ConditionalPvpPolicy;
import com.hushkisses.spacesurvival.social.pvp.PvpRuntimeState;
import com.hushkisses.spacesurvival.social.sanction.SanctionExecutor;
import com.hushkisses.spacesurvival.social.sanction.SanctionStateRegistry;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class SpaceSurvivalPlugin extends JavaPlugin {

    private PluginConfiguration configuration;
    private LobbyService lobbyService;
    private RoleRegistry roleRegistry;
    private RoleSelectionService roleSelectionService;
    private OpeningBriefingUi openingBriefingUi;
    private RoleSelectionUi roleSelectionUi;
    private GameRuntimeService gameRuntimeService;
    private ShipState shipState;
    private FacilityRegistry facilityRegistry;

    private FacilityActionRegistry facilityActionRegistry;
    private EngineeringFacilityService engineeringFacilityService;
    private MedicalFacilityService medicalFacilityService;

    private ResourceLedger resourceLedger;
    private ProcessingRegistry processingRegistry;
    private ProcessingService processingService;
    private ResourceItemProvider resourceItemProvider;

    private ObjectiveRegistry objectiveRegistry;
    private ObjectiveEngine objectiveEngine;
    private SecretMissionService secretMissionService;

    private GameEventRegistry gameEventRegistry;
    private GameEventEngine gameEventEngine;
    private GameEventRuntimeState gameEventRuntimeState;
    private GameEventContext gameEventContext;

    private MeetingService meetingService;
    private SanctionStateRegistry sanctionStateRegistry;
    private SanctionExecutor sanctionExecutor;
    private PvpRuntimeState pvpRuntimeState;
    private ConditionalPvpPolicy conditionalPvpPolicy;

    private RadioRuntimeState radioRuntimeState;
    private RadioService radioService;
    private SimpleVoiceChatBridge simpleVoiceChatBridge;

    private ScenarioEngine scenarioEngine;
    private InfectionService infectionService;
    private InfectionScenarioService infectionScenarioService;

    @Override
    public void onEnable() {
        configuration = new PluginConfigurationLoader(this).load();
        lobbyService = new LobbyService(configuration.game());

        roleRegistry = DefaultRoleCatalog.createRegistry();
        roleSelectionService = new RoleSelectionService(roleRegistry);

        openingBriefingUi = new OpeningBriefingUi();
        roleSelectionUi = new RoleSelectionUi(this, roleSelectionService, roleRegistry);

        shipState = ShipState.healthy();
        facilityRegistry = DefaultFacilityCatalog.createRegistry();
        facilityActionRegistry = DefaultFacilityActionCatalog.createRegistry();
        engineeringFacilityService = new EngineeringFacilityService(facilityRegistry, shipState);
        medicalFacilityService = new MedicalFacilityService(facilityRegistry);

        resourceLedger = new ResourceLedger();
        processingRegistry = DefaultProcessingCatalog.createRegistry();
        processingService = new ProcessingService();
        resourceItemProvider = new DefaultResourceItemProvider(new ItemsAdderBridge());

        objectiveRegistry = DefaultObjectiveCatalog.createRegistry();
        resetObjectiveRuntime();

        gameEventRegistry = DefaultGameEventCatalog.createRegistry();
        gameEventEngine = new GameEventEngine();
        gameEventRuntimeState = new GameEventRuntimeState();
        gameEventContext = new GameEventContext(
                shipState,
                facilityRegistry,
                resourceLedger,
                gameEventRuntimeState
        );

        gameRuntimeService = new GameRuntimeService(
                this,
                configuration.balance().targetMatchMinutes(),
                shipState
        );

        meetingService = new MeetingService(
                facilityRegistry,
                shipState,
                java.time.Duration.ofMinutes(3)
        );
        sanctionStateRegistry = new SanctionStateRegistry();
        sanctionExecutor = new SanctionExecutor(sanctionStateRegistry);
        pvpRuntimeState = new PvpRuntimeState();
        conditionalPvpPolicy = new ConditionalPvpPolicy();

        radioRuntimeState = new RadioRuntimeState();
        radioService = new RadioService(radioRuntimeState, new CommunicationPolicy());
        simpleVoiceChatBridge = new SimpleVoiceChatBridge();

        scenarioEngine = new ScenarioEngine();
        infectionService = new InfectionService();
        infectionScenarioService = new InfectionScenarioService();

        registerCommands();
        getServer().getPluginManager().registerEvents(
                new LobbyConnectionListener(lobbyService),
                this
        );
        getServer().getPluginManager().registerEvents(
                new OpeningUiListener(
                        openingBriefingUi,
                        roleSelectionUi,
                        roleSelectionService,
                        roleRegistry
                ),
                this
        );
        getServer().getPluginManager().registerEvents(
                new ConditionalPvpListener(this),
                this
        );

        getLogger().info("SpaceSurvival enabled. core=" + BootstrapMarker.moduleName());
        getLogger().info(
                "Configuration loaded: players="
                        + configuration.game().minPlayers()
                        + "-"
                        + configuration.game().maxPlayers()
                        + ", targetMatchMinutes="
                        + configuration.balance().targetMatchMinutes()
                        + ", returnHoldSeconds="
                        + configuration.balance().returnHoldSeconds()
        );
        getLogger().info("Role registry loaded: " + roleRegistry.size() + " roles");
        getLogger().info("Facility registry loaded: " + facilityRegistry.size() + " facilities");
        getLogger().info("Facility actions loaded: " + facilityActionRegistry.size());
        getLogger().info("Objectives loaded: " + objectiveRegistry.size());
        getLogger().info("Events loaded: " + gameEventRegistry.all().size());
        getLogger().info(
                "ItemsAdder bridge available: " + resourceItemProvider.customItemsAvailable()
        );
        getLogger().info(
                "Simple Voice Chat bridge: " + simpleVoiceChatBridge.status()
        );
    }

    @Override
    public void onDisable() {
        getLogger().info("SpaceSurvival disabled.");
    }

    public PluginConfiguration configuration() {
        return require(configuration, "Plugin configuration");
    }

    public LobbyService lobbyService() {
        return require(lobbyService, "Lobby service");
    }

    public RoleRegistry roleRegistry() {
        return require(roleRegistry, "Role registry");
    }

    public RoleSelectionService roleSelectionService() {
        return require(roleSelectionService, "Role selection service");
    }

    public OpeningBriefingUi openingBriefingUi() {
        return require(openingBriefingUi, "Opening briefing UI");
    }

    public RoleSelectionUi roleSelectionUi() {
        return require(roleSelectionUi, "Role selection UI");
    }

    public GameRuntimeService gameRuntimeService() {
        return require(gameRuntimeService, "Game runtime service");
    }

    public ShipState shipState() {
        return require(shipState, "Ship state");
    }

    public FacilityRegistry facilityRegistry() {
        return require(facilityRegistry, "Facility registry");
    }

    public FacilityActionRegistry facilityActionRegistry() {
        return require(facilityActionRegistry, "Facility action registry");
    }

    public EngineeringFacilityService engineeringFacilityService() {
        return require(engineeringFacilityService, "Engineering facility service");
    }

    public MedicalFacilityService medicalFacilityService() {
        return require(medicalFacilityService, "Medical facility service");
    }

    public ResourceLedger resourceLedger() {
        return require(resourceLedger, "Resource ledger");
    }

    public ProcessingRegistry processingRegistry() {
        return require(processingRegistry, "Processing registry");
    }

    public ProcessingService processingService() {
        return require(processingService, "Processing service");
    }

    public ResourceItemProvider resourceItemProvider() {
        return require(resourceItemProvider, "Resource item provider");
    }

    public ObjectiveRegistry objectiveRegistry() {
        return require(objectiveRegistry, "Objective registry");
    }

    public ObjectiveEngine objectiveEngine() {
        return require(objectiveEngine, "Objective engine");
    }

    public SecretMissionService secretMissionService() {
        return require(secretMissionService, "Secret mission service");
    }

    public GameEventRegistry gameEventRegistry() {
        return require(gameEventRegistry, "Game event registry");
    }

    public GameEventEngine gameEventEngine() {
        return require(gameEventEngine, "Game event engine");
    }

    public GameEventRuntimeState gameEventRuntimeState() {
        return require(gameEventRuntimeState, "Game event runtime state");
    }

    public GameEventContext gameEventContext() {
        return require(gameEventContext, "Game event context");
    }

    public MeetingService meetingService() {
        return require(meetingService, "Meeting service");
    }

    public SanctionStateRegistry sanctionStateRegistry() {
        return require(sanctionStateRegistry, "Sanction state registry");
    }

    public SanctionExecutor sanctionExecutor() {
        return require(sanctionExecutor, "Sanction executor");
    }

    public PvpRuntimeState pvpRuntimeState() {
        return require(pvpRuntimeState, "PvP runtime state");
    }

    public ConditionalPvpPolicy conditionalPvpPolicy() {
        return require(conditionalPvpPolicy, "Conditional PvP policy");
    }

    public RadioRuntimeState radioRuntimeState() {
        return require(radioRuntimeState, "Radio runtime state");
    }

    public RadioService radioService() {
        return require(radioService, "Radio service");
    }

    public SimpleVoiceChatBridge simpleVoiceChatBridge() {
        return require(simpleVoiceChatBridge, "Simple Voice Chat bridge");
    }

    public ScenarioEngine scenarioEngine() {
        return require(scenarioEngine, "Scenario engine");
    }

    public InfectionService infectionService() {
        return require(infectionService, "Infection service");
    }

    public InfectionScenarioService infectionScenarioService() {
        return require(infectionScenarioService, "Infection scenario service");
    }

    public void resetScenarioRuntime() {
        scenarioEngine.clear();
        infectionService = new InfectionService();
        pvpRuntimeState = new PvpRuntimeState();
    }

    public void resetShipState() {
        shipState().reset();
    }

    public void resetObjectiveRuntime() {
        objectiveEngine = new ObjectiveEngine();
        secretMissionService = new SecretMissionService(objectiveEngine);
    }

    private void registerCommands() {
        PluginCommand command = getCommand("space");
        if (command == null) {
            throw new IllegalStateException("Command 'space' is missing from plugin.yml");
        }
        command.setExecutor(new SpaceCommand(this));
    }

    private static <T> T require(T value, String name) {
        if (value == null) {
            throw new IllegalStateException(name + " is not initialized yet");
        }
        return value;
    }
}
