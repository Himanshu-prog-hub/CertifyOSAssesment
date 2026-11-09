public class WebResponse
{
    private String url;
    private String name;

    public WebResponse(String url, String name)
    {
        this.url = url;
        this.name = name;
    }

    public String getUrl()
    {
        return url;
    }

    public String getName()
    {
        return name;
    }
}