package behaviours;

import agents.CoordinatorAgent2;
import helpers.AgentHelper;
import helpers.Log;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import messages.YouAreDistrictLeaderMessage;

import java.util.Random;

public class CoordinatorSelectDistrictLeadersBehaviour extends OneShotBehaviour {
    private final Random random;
    private CoordinatorAgent2 agent;

    public CoordinatorSelectDistrictLeadersBehaviour(CoordinatorAgent2 agent) {
        super(agent);

        this.agent = agent;
        random = new Random();
    }

    @Override
    public void action() {
        var districts = agent.getAllDistricts();
        for (int district : districts) {
            RandomlyChooseLeaderAndSendMessage(district);
        }
    }

    private void RandomlyChooseLeaderAndSendMessage(int district) {
        var agents = AgentHelper.findAgents(agent, district);
        var leaderIndex = ChooseLeaderIndex(agents.length);

        SendMessageToLeader(agents[leaderIndex]);
    }

    private int ChooseLeaderIndex(int length) {
        return random.nextInt(length);
    }

    private void SendMessageToLeader(DFAgentDescription leader)
    {
        Log.fromAgent(this.agent, "Send leader message to " + leader.getName());
        agent.send(new YouAreDistrictLeaderMessage(leader));
    }
}
