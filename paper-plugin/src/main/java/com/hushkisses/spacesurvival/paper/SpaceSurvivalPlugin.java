package com.hushkisses.spacesurvival.paper;

import com.hushkisses.spacesurvival.communication.CommunicationPolicy;
import com.hushkisses.spacesurvival.communication.RadioRuntimeState;
import com.hushkisses.spacesurvival.communication.RadioService;
import com.hushkisses.spacesurvival.core.BootstrapMarker;
import com.hushkisses.spacesurvival.death.DeathService;
import com.hushkisses.spacesurvival.death.InfectedPlayerService;
import com.hushkisses.spacesurvival.ending.CommonContributionLedger;
import com.hushkisses.spacesurvival.ending.FinalHoldService;
import com.hushkisses.spacesurvival.ending.ReturnObjectiveService;
import com.hushkisses.spacesurvival.ending.ReturnRequirements;
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
import com.hushkisses.spacesurvival.infection.InfectionService;
import com.hushkisses.spacesurvival.integration.itemsadder.ItemsAdderBridge;
import com.hushkisses.spacesurvival.integration.modelengine.ModelEngineBridge;
import com.hushkisses.spacesurvival.integration.mythicmobs.MythicMobsBridge;
import com.hushkisses.spacesurvival.integration.voicechat.SimpleVoiceChatBridge;
import com.hushkisses.spacesurvival.lobby.LobbyService;
import com.hushkisses.spacesurvival.objective.DefaultObjectiveCatalog;
import com.hushkisses.spacesurvival.objective.ObjectiveEngine;
import com.hushkisses.spacesurvival.objective.ObjectiveRegistry;
import com.hushkisses.spacesurvival.objective.secret.SecretMissionService;
import com.hushkisses.spacesurvival.paper.config.PluginConfiguration;
import com.hushkisses.spacesurvival.paper.config.PluginConfigurationLoader;
import com.hushkisses.spacesurvival.paper.death.PlayerDeathStateListener;
import com.hushkisses.spacesurvival.paper.ending.EndingRuntimeService;
import com.hushkisses.spacesurvival.paper.ending.MatchResultRuntimeService;
import com.hushkisses.spacesurvival.paper.event.IncidentDirector;
import com.hushkisses.spacesurvival.paper.facility.FacilityActionExecutor;
import com.hushkisses.spacesurvival.paper.facility.FacilityInteractionListener;
import com.hushkisses.spacesurvival.paper.facility.FacilityMenuService;
import com.hushkisses.spacesurvival.paper.facility.FacilityTerminalRegistry;
import com.hushkisses.spacesurvival.paper.item.DefaultResourceItemProvider;
import com.hushkisses.spacesurvival.paper.item.FunctionalItemListener;
import com.hushkisses.spacesurvival.paper.item.FunctionalItemService;
import com.hushkisses.spacesurvival.paper.item.StarterKitService;
import com.hushkisses.spacesurvival.paper.item.ResourceItemProvider;
import com.hushkisses.spacesurvival.paper.lobby.LobbyConnectionListener;
import com.hushkisses.spacesurvival.paper.map.physical.PaperShipWorldService;
import com.hushkisses.spacesurvival.paper.map.physical.PhysicalConnectionController;
import com.hushkisses.spacesurvival.paper.map.physical.ShipPortalListener;
import com.hushkisses.spacesurvival.paper.match.MatchOrchestrator;
import com.hushkisses.spacesurvival.paper.pve.DefaultPveMobSpawner;
import com.hushkisses.spacesurvival.paper.pve.PveMobSpawner;
import com.hushkisses.spacesurvival.paper.pvp.ConditionalPvpListener;
import com.hushkisses.spacesurvival.paper.resource.ResourcePhysicalItemService;
import com.hushkisses.spacesurvival.paper.resource.ResourceWorldService;
import com.hushkisses.spacesurvival.paper.runtime.GameRuntimeService;
import com.hushkisses.spacesurvival.paper.social.MeetingGuiService;
import com.hushkisses.spacesurvival.paper.social.SanctionEnforcementListener;
import com.hushkisses.spacesurvival.paper.telemetry.MatchTelemetryService;
import com.hushkisses.spacesurvival.paper.ui.OpeningBriefingUi;
import com.hushkisses.spacesurvival.paper.ui.OpeningUiListener;
import com.hushkisses.spacesurvival.paper.ui.MatchHudService;
import com.hushkisses.spacesurvival.paper.ui.RoleSelectionUi;
import com.hushkisses.spacesurvival.resource.ResourceLedger;
import com.hushkisses.spacesurvival.resource.processing.DefaultProcessingCatalog;
import com.hushkisses.spacesurvival.resource.processing.ProcessingRegistry;
import com.hushkisses.spacesurvival.resource.processing.ProcessingService;
import com.hushkisses.spacesurvival.result.ResultEvaluator;
import com.hushkisses.spacesurvival.result.ResultScoringConfig;
import com.hushkisses.spacesurvival.role.DefaultRoleCatalog;
import com.hushkisses.spacesurvival.role.RoleRegistry;
import com.hushkisses.spacesurvival.role.selection.RoleSelectionService;
import com.hushkisses.spacesurvival.scenario.InfectionScenarioService;
import com.hushkisses.spacesurvival.scenario.ScenarioEngine;
import com.hushkisses.spacesurvival.ship.ShipState;
import com.hushkisses.spacesurvival.social.meeting.MeetingService;
import com.hushkisses.spacesurvival.social.pvp.ConditionalPvpPolicy;
import com.hushkisses.spacesurvival.social.pvp.PvpRuntimeState;
import com.hushkisses.spacesurvival.social.sanction.SanctionExecutor;
import com.hushkisses.spacesurvival.social.sanction.SanctionStateRegistry;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;

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
    private FunctionalItemService functionalItemService;
    private StarterKitService starterKitService;
    private ResourcePhysicalItemService resourcePhysicalItemService;
    private ResourceWorldService resourceWorldService;

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
    private MeetingGuiService meetingGuiService;
    private PvpRuntimeState pvpRuntimeState;
    private ConditionalPvpPolicy conditionalPvpPolicy;

    private RadioRuntimeState radioRuntimeState;
    private RadioService radioService;
    private SimpleVoiceChatBridge simpleVoiceChatBridge;

    private ScenarioEngine scenarioEngine;
    private InfectionService infectionService;
    private InfectionScenarioService infectionScenarioService;

    private DeathService deathService;
    private InfectedPlayerService infectedPlayerService;

    private MythicMobsBridge mythicMobsBridge;
    private ModelEngineBridge modelEngineBridge;
    private PveMobSpawner pveMobSpawner;

    private ReturnObjectiveService returnObjectiveService;
    private FinalHoldService finalHoldService;
    private EndingRuntimeService endingRuntimeService;
    private CommonContributionLedger commonContributionLedger;
    private ResultEvaluator resultEvaluator;
    private MatchResultRuntimeService matchResultRuntimeService;

    private FacilityTerminalRegistry facilityTerminalRegistry;
    private PhysicalConnectionController physicalConnectionController;
    private FacilityActionExecutor facilityActionExecutor;
    private FacilityMenuService facilityMenuService;

    private PaperShipWorldService shipWorldService;
    private MatchOrchestrator matchOrchestrator;
    private MatchHudService matchHudService;
    private IncidentDirector incidentDirector;
    private MatchTelemetryService telemetryService;

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
        ItemsAdderBridge itemsAdderBridge = new ItemsAdderBridge();
        resourceItemProvider = new DefaultResourceItemProvider(itemsAdderBridge);
        functionalItemService = new FunctionalItemService(this, itemsAdderBridge);
        starterKitService = new StarterKitService(this, functionalItemService);
        resourcePhysicalItemService = new ResourcePhysicalItemService(
                this,
                resourceItemProvider
        );
        resourceWorldService = new ResourceWorldService(resourcePhysicalItemService);

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
                Duration.ofMinutes(3)
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

        deathService = new DeathService();
        infectedPlayerService = new InfectedPlayerService();

        mythicMobsBridge = new MythicMobsBridge();
        modelEngineBridge = new ModelEngineBridge();
        pveMobSpawner = new DefaultPveMobSpawner(mythicMobsBridge, modelEngineBridge);

        resetEndingRuntime();

        facilityTerminalRegistry = new FacilityTerminalRegistry();
        physicalConnectionController = new PhysicalConnectionController();
        facilityActionExecutor = new FacilityActionExecutor(
                this,
                physicalConnectionController
        );
        facilityMenuService = new FacilityMenuService(
                this,
                facilityActionExecutor
        );

        shipWorldService = new PaperShipWorldService(
                this,
                facilityTerminalRegistry,
                physicalConnectionController
        );
        matchOrchestrator = new MatchOrchestrator(this, shipWorldService);
        matchHudService = new MatchHudService(this, shipWorldService);
        incidentDirector = new IncidentDirector(
                this,
                physicalConnectionController
        );
        telemetryService = new MatchTelemetryService(this);
        meetingGuiService = new MeetingGuiService(this);

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
                        roleRegistry,
                        matchOrchestrator::tryActivateIfReady
                ),
                this
        );
        getServer().getPluginManager().registerEvents(
                new ConditionalPvpListener(this),
                this
        );
        getServer().getPluginManager().registerEvents(
                new SanctionEnforcementListener(this),
                this
        );
        getServer().getPluginManager().registerEvents(
                new PlayerDeathStateListener(this),
                this
        );
        getServer().getPluginManager().registerEvents(
                new ShipPortalListener(
                        this,
                        shipWorldService,
                        physicalConnectionController
                ),
                this
        );
        getServer().getPluginManager().registerEvents(
                new FacilityInteractionListener(
                        facilityTerminalRegistry,
                        facilityMenuService
                ),
                this
        );
        getServer().getPluginManager().registerEvents(
                facilityMenuService,
                this
        );
        getServer().getPluginManager().registerEvents(
                new FunctionalItemListener(this, functionalItemService),
                this
        );
        getServer().getPluginManager().registerEvents(
                meetingGuiService,
                this
        );

        matchHudService.start();

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
        getLogger().info("PvE backend: " + pveMobSpawner.backendStatus());
    }

    @Override
    public void onDisable() {
        if (matchHudService != null) {
            matchHudService.stop();
        }
        if (incidentDirector != null) {
            incidentDirector.stop();
        }
        if (telemetryService != null && telemetryService.snapshot().active()) {
            telemetryService.finish("server_shutdown");
        }
        getLogger().info("SpaceSurvival disabled.");
    }

    public PluginConfiguration configuration() { return require(configuration, "Plugin configuration"); }
    public LobbyService lobbyService() { return require(lobbyService, "Lobby service"); }
    public RoleRegistry roleRegistry() { return require(roleRegistry, "Role registry"); }
    public RoleSelectionService roleSelectionService() { return require(roleSelectionService, "Role selection service"); }
    public OpeningBriefingUi openingBriefingUi() { return require(openingBriefingUi, "Opening briefing UI"); }
    public RoleSelectionUi roleSelectionUi() { return require(roleSelectionUi, "Role selection UI"); }
    public GameRuntimeService gameRuntimeService() { return require(gameRuntimeService, "Game runtime service"); }
    public ShipState shipState() { return require(shipState, "Ship state"); }
    public FacilityRegistry facilityRegistry() { return require(facilityRegistry, "Facility registry"); }
    public FacilityActionRegistry facilityActionRegistry() { return require(facilityActionRegistry, "Facility action registry"); }
    public EngineeringFacilityService engineeringFacilityService() { return require(engineeringFacilityService, "Engineering facility service"); }
    public MedicalFacilityService medicalFacilityService() { return require(medicalFacilityService, "Medical facility service"); }
    public ResourceLedger resourceLedger() { return require(resourceLedger, "Resource ledger"); }
    public ProcessingRegistry processingRegistry() { return require(processingRegistry, "Processing registry"); }
    public ProcessingService processingService() { return require(processingService, "Processing service"); }
    public ResourceItemProvider resourceItemProvider() { return require(resourceItemProvider, "Resource item provider"); }
    public FunctionalItemService functionalItemService() { return require(functionalItemService, "Functional item service"); }
    public StarterKitService starterKitService() { return require(starterKitService, "Starter kit service"); }
    public ResourcePhysicalItemService resourcePhysicalItemService() { return require(resourcePhysicalItemService, "Physical resource item service"); }
    public ResourceWorldService resourceWorldService() { return require(resourceWorldService, "Resource world service"); }
    public ObjectiveRegistry objectiveRegistry() { return require(objectiveRegistry, "Objective registry"); }
    public ObjectiveEngine objectiveEngine() { return require(objectiveEngine, "Objective engine"); }
    public SecretMissionService secretMissionService() { return require(secretMissionService, "Secret mission service"); }
    public GameEventRegistry gameEventRegistry() { return require(gameEventRegistry, "Game event registry"); }
    public GameEventEngine gameEventEngine() { return require(gameEventEngine, "Game event engine"); }
    public GameEventRuntimeState gameEventRuntimeState() { return require(gameEventRuntimeState, "Game event runtime state"); }
    public GameEventContext gameEventContext() { return require(gameEventContext, "Game event context"); }
    public MeetingService meetingService() { return require(meetingService, "Meeting service"); }
    public SanctionStateRegistry sanctionStateRegistry() { return require(sanctionStateRegistry, "Sanction state registry"); }
    public SanctionExecutor sanctionExecutor() { return require(sanctionExecutor, "Sanction executor"); }
    public MeetingGuiService meetingGuiService() { return require(meetingGuiService, "Meeting GUI service"); }
    public PvpRuntimeState pvpRuntimeState() { return require(pvpRuntimeState, "PvP runtime state"); }
    public ConditionalPvpPolicy conditionalPvpPolicy() { return require(conditionalPvpPolicy, "Conditional PvP policy"); }
    public RadioRuntimeState radioRuntimeState() { return require(radioRuntimeState, "Radio runtime state"); }
    public RadioService radioService() { return require(radioService, "Radio service"); }
    public SimpleVoiceChatBridge simpleVoiceChatBridge() { return require(simpleVoiceChatBridge, "Simple Voice Chat bridge"); }
    public ScenarioEngine scenarioEngine() { return require(scenarioEngine, "Scenario engine"); }
    public InfectionService infectionService() { return require(infectionService, "Infection service"); }
    public InfectionScenarioService infectionScenarioService() { return require(infectionScenarioService, "Infection scenario service"); }
    public DeathService deathService() { return require(deathService, "Death service"); }
    public InfectedPlayerService infectedPlayerService() { return require(infectedPlayerService, "Infected player service"); }
    public MythicMobsBridge mythicMobsBridge() { return require(mythicMobsBridge, "MythicMobs bridge"); }
    public ModelEngineBridge modelEngineBridge() { return require(modelEngineBridge, "ModelEngine bridge"); }
    public PveMobSpawner pveMobSpawner() { return require(pveMobSpawner, "PvE mob spawner"); }
    public ReturnObjectiveService returnObjectiveService() { return require(returnObjectiveService, "Return objective service"); }
    public FinalHoldService finalHoldService() { return require(finalHoldService, "Final hold service"); }
    public EndingRuntimeService endingRuntimeService() { return require(endingRuntimeService, "Ending runtime service"); }
    public CommonContributionLedger commonContributionLedger() { return require(commonContributionLedger, "Common contribution ledger"); }
    public ResultEvaluator resultEvaluator() { return require(resultEvaluator, "Result evaluator"); }
    public MatchResultRuntimeService matchResultRuntimeService() { return require(matchResultRuntimeService, "Match result runtime service"); }
    public PaperShipWorldService shipWorldService() { return require(shipWorldService, "Ship world service"); }
    public MatchOrchestrator matchOrchestrator() { return require(matchOrchestrator, "Match orchestrator"); }
    public MatchHudService matchHudService() { return require(matchHudService, "Match HUD service"); }
    public FacilityTerminalRegistry facilityTerminalRegistry() { return require(facilityTerminalRegistry, "Facility terminal registry"); }
    public PhysicalConnectionController physicalConnectionController() { return require(physicalConnectionController, "Physical connection controller"); }
    public FacilityActionExecutor facilityActionExecutor() { return require(facilityActionExecutor, "Facility action executor"); }
    public FacilityMenuService facilityMenuService() { return require(facilityMenuService, "Facility menu service"); }
    public IncidentDirector incidentDirector() { return require(incidentDirector, "Incident director"); }
    public MatchTelemetryService telemetryService() { return require(telemetryService, "Telemetry service"); }

    public void resetScenarioRuntime() {
        scenarioEngine.clear();
        infectionService = new InfectionService();
        pvpRuntimeState = new PvpRuntimeState();
    }

    public void resetForNewMatch() {
        if (incidentDirector != null) {
            incidentDirector.stop();
        }
        if (gameRuntimeService != null && gameRuntimeService.isRunning()) {
            gameRuntimeService.stop();
        }

        roleSelectionService.reset();
        if (meetingGuiService != null) {
            meetingGuiService.resetRuntime();
        }
        shipState.reset();
        facilityRegistry.resetAll();
        resourceLedger.clear();
        gameEventRuntimeState.clear();
        radioRuntimeState.reset();
        deathService.clear();
        infectedPlayerService.clear();
        sanctionStateRegistry.clear();

        resetObjectiveRuntime();

        scenarioEngine.clear();
        infectionService = new InfectionService();
        pvpRuntimeState = new PvpRuntimeState();

        meetingService = new MeetingService(
                facilityRegistry,
                shipState,
                Duration.ofMinutes(3)
        );

        gameRuntimeService = new GameRuntimeService(
                this,
                configuration.balance().targetMatchMinutes(),
                shipState
        );

        resetEndingRuntime();
    }

    public void resetShipState() {
        shipState().reset();
    }

    public void resetObjectiveRuntime() {
        objectiveEngine = new ObjectiveEngine();
        secretMissionService = new SecretMissionService(objectiveEngine);
    }

    public void resetEndingRuntime() {
        resetEndingRuntime(Duration.ofSeconds(configuration.balance().returnHoldSeconds()));
    }

    public void resetEndingRuntime(Duration holdDuration) {
        if (holdDuration == null || holdDuration.isZero() || holdDuration.isNegative()) {
            throw new IllegalArgumentException("holdDuration must be positive");
        }

        returnObjectiveService = new ReturnObjectiveService(
                shipState,
                facilityRegistry,
                ReturnRequirements.developmentDefaults()
        );
        finalHoldService = new FinalHoldService(holdDuration);
        endingRuntimeService = new EndingRuntimeService(
                this,
                returnObjectiveService,
                finalHoldService
        );

        ResultScoringConfig scoring = ResultScoringConfig.developmentDefaults();
        commonContributionLedger = new CommonContributionLedger(
                scoring.maxCommonContribution()
        );
        resultEvaluator = new ResultEvaluator(scoring);
        matchResultRuntimeService = new MatchResultRuntimeService(
                this,
                commonContributionLedger,
                resultEvaluator
        );
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
