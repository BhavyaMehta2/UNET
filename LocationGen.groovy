class LocationGen{
    public HashMap generate(int nodes, int radius, int depthBase, int depthData)
    {
        Random random = new Random()
        def nodeLocation = [:]

        nodeLocation[1] = [ 0.m, 0.m, -depthBase.m]

        for(int i = 2; i<=nodes; i++)
        {
            nodeLocation[i] = [ random.nextInt(2*radius)-radius.m, random.nextInt(2*radius)-radius.m, -(depthData).m]
        }

        return nodeLocation
    }
}